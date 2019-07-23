package net.nullsum.freedoom;

import android.widget.ProgressBar;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// Download wad files either directly or in zip files; unzip if necessary
// and then copy into various locations in the doom package
// URLs must be https for the url connection code to work
// code by rdk with mods/snippets taken from various websites (see comments below)

// use class AppSettingsfreedoomBaseDir to fetch app dir where we want to copy files
//

public class WadDownloader extends Fragment
{
    public static String TAG = "WadDownloader";
    TextView textView;
    EditText urlText;
    //RadioButton wadRadio, modRadio;
    WadType wType = WadType.UNKNOWN_WAD;
    long downloadReference;
    AsyncTask downloadUrlTask;
    ProgressBar progressBar;
    private String fullPath;
    private String appDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    } // onCreate

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View mainView = inflater.inflate(R.layout.download_layout, null);

        textView = (TextView) findViewById(R.id.download_text_view);
        urlText = (EditText) findViewById(R.id.EnterURL);
        //wadRadio = (RadioButton)findViewById(R.id.radio_wad);
        //modRadio = (RadioButton)findViewById(R.id.radio_mod);

        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.INVISIBLE);

    } // onCreateView

    // user mashed "Download"
    public void onDownload(View view)
    {
        String theUrl;
        Uri theUri;

        Log.d(TAG,"Downloading now");

        // reset the text area
        clearText();

        // get the URL
        theUrl = urlText.getText().toString();
        theUri = Uri.parse(theUrl);

        Log.d(TAG,"Fetching url "+theUrl);

        appendText("Fetching url "+theUrl+"\n");

        // get the file type from radio box
        // file type already set from radio callback

        downloadUrlTask = new DownloadFileFromURL().execute(theUrl);

    }

    // private enumerated type for identifying wad files and mods
    // just used internally
    private enum WadType {
        UNKNOWN_WAD, WAD, IWAD, PWAD, MODWAD
    }

    public void clearText()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText("");
            }
        });

    } // clear textView but do so on UI thread

    protected void appendText(final String aStr)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(aStr);
            }
        });

    } // apendText

    protected WadType getWadType(String filename)
    {
        FileInputStream f;
        byte[] b = new byte[5];

        try {

            f = new FileInputStream(filename);
            f.read(b,0,5);
            f.close();

            switch (b[0]) {
                case 'I':
                    Log.d(TAG,"Wad file is IWAD");
                    return WadType.IWAD;
                case 'P':
                    Log.d(TAG,"Wad file is PWAD");
                    return WadType.PWAD;
                default:
                    Log.d(TAG,"Probably a mod file");
                    return WadType.MODWAD;
            }

        } catch (Exception e) {
            Log.d(TAG,"getWadType: file io exception "+e.getMessage());
            return WadType.UNKNOWN_WAD;
        }

    } // getWadType()

    // Where did we steal code from?
    //   https://www.concretepage.com/android/android-asynctask-example-with-progress-bar
    //   https://stackoverflow.com/questions/15758856/android-how-to-download-file-from-webserver/
    //   https://stackoverflow.com/questions/7485114/how-to-zip-and-unzip-the-files
    //   https://stackoverflow.com/questions/4050087/how-to-obtain-the-last-path-segment-of-an-uri

    class DownloadFileFromURL extends AsyncTask<String, Integer, String>
    {
        protected String doInBackground(String... params)
        {
            String urlStr = params[0];
            Log.d(TAG,"doInBackground: fetching URL "+params[0]);

            try {
                URL url = new URL(params[0]);
                URI uri = new URI(params[0]);
                File aFile;

                String path = uri.getPath();
                String destFilename = path.substring(path.lastIndexOf("/")+1);

                Log.d(TAG,"doInBackground: last segment of URL is "+destFilename);
                appendText("Destination filename is "+destFilename+"\n");

                URLConnection conection = url.openConnection();
                conection.connect();

                publishProgress(5);

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),8192);

                // android.content.Context.getFilesDir();
                // iwad goes into base dir
                // pwad goes into wads dir
                // mod goes into mods directory
                // Output stream

                //appDir = getApplicationContext().getFilesDir()+"/";
                appDir = AppSettings.freedoomBaseDir;
                fullPath = appDir+destFilename;

                Log.d(TAG,"doInBackground: appDir is "+appDir);
                appendText("App directory is "+appDir+"\n");

                Log.d(TAG,"Downloading to "+fullPath);

                OutputStream output = new FileOutputStream(
                        //Environment.getExternalStorageDirectory().toString()
                        fullPath);

                byte data[] = new byte[1024];

                long total = 0;
                int count;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                Log.d(TAG,"doInBackground: downloaded "+total+" bytes");
                appendText("Downloaded "+total+" bytes\n");

                publishProgress(10);

                // closing streams
                output.close();
                input.close();

                publishProgress(15);

                // if its a zip file, we need to unzip XXX
                String unzippedFile;
                WadType aWadType;

                // move the file to wherever we want it XXX
                // if its a zip file, unzip
                if (urlStr.endsWith(".zip") == true) {
                    publishProgress(20);
                    Log.d(TAG,"doInBackground: unzipping the downloaded file");
                    appendText("Unzipping the file . . . please be patient\n");
                    unzippedFile = unzipFileBlock(20,fullPath,appDir);
                    publishProgress(80);

                    // remove the zip file
                    aFile = new File(fullPath);
                    if (aFile.delete() == true) {
                        publishProgress(85);
                        appendText("Cleaning up . . .\n");
                        Log.d(TAG,"doInBackground: deleted file "+fullPath);
                    }
                    else {
                        appendText("Problem deleting file "+fullPath+"\n");
                        Log.d(TAG,"doInBackground: problem deleting "+fullPath);
                    }
                    publishProgress(90);
                }
                else {
                    unzippedFile = fullPath;
                    publishProgress(90);
                }

                // where is resulting file?! XXX
                Log.d(TAG,"doInBackground: the resulting wad file is "+unzippedFile);
                appendText("Resulting wad file is "+unzippedFile+"\n");

                // check file type and see where to put it
                aWadType = getWadType(unzippedFile);

                publishProgress(95);

                // move the file to its destination XXX
                aFile = new File(unzippedFile);

                Log.d(TAG,"doInBackground: resulting wad file is "+aFile.getName());
                String newFilename;

                switch (aWadType) {
                    case IWAD:
                        // move into main appDir
                        appendText("Download is an IWAD file\n");
                        newFilename = new String(appDir+aFile.getName());
                        Log.d(TAG,"doInBackground: iwad location to move to is "+newFilename);
                        appendText("Moving to "+newFilename+"\n");
                        aFile.renameTo(new File(newFilename));
                        break;
                    case PWAD:
                        // move into pwad directory
                        appendText("Download is a PWAD file\n");
                        newFilename = new String(appDir+"wads/"+aFile.getName());
                        Log.d(TAG,"doInBackground: pwad location to move to is "+newFilename);
                        appendText("Moving to "+newFilename+"\n");
                        aFile.renameTo(new File(newFilename));
                        break;

                    // for now, don't bother with MODs
                    case MODWAD:
                        // move into mods directory
                        appendText("Download is a mod file, skipping\n");
                        //newFilename = new String(appDir+"mods/"+aFile.getName());
                        //Log.d(TAG,"doInBackground: mods location to move to is "+newFilename);
                        //aFile.renameTo(new File(newFilename));
                        break;
                    case UNKNOWN_WAD:
                    default:
                        // if user specified it was a mod file, then copy
                        //if (wType == WadType.MODWAD) {
                        // move into mods directory
                        //   appendText("Download is (inferred) a mod file, skipping\n");
                        //   newFilename = new String(appDir+"mods/"+aFile.getName());
                        //   Log.d(TAG,"doInBackground: mods location to move to is "+newFilename);
                        //   aFile.renameTo(new File(newFilename));
                        //}
                        //else {
                        //  Log.d(TAG, "doInBackground: I don't know what to do with " + unzippedFile);
                        //  appendText("I don't know what to do with " + unzippedFile + "\n");
                        //}
                        break;

                } // switch wad type

                // if it was zip file, remove the zip directory after we move the file XXX

                publishProgress(100);

                Log.d(TAG,"doInBackground: finished");
                appendText("Finished\n");

            } catch (Exception e) {
                Log.e(TAG,"error downloading "+e.getMessage());
                appendText("Error downloading "+e+", skipping");
            }

            publishProgress(100);

            return new String("");

        } // doInBackground

        protected void onPostExecute(String result) {
            Log.d(TAG,"onPostExecute starting");
            progressBar.setVisibility(View.INVISIBLE);
        }
        protected void onPreExecute() {
            Log.d(TAG,"onPreExecute starting");
            progressBar.setVisibility(View.VISIBLE);


        }
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);

        }

        // unzip can take progressBar from 20-80
        private String unzipFile(int progressVal, String zipFile, String location)
        {
            String returnFilename = new String("");

            // how do we know if we are unzipping a mod zip file?
            // if so, we need to return the name of the directory we unzip into XXX

            try {
                File f = new File(location);
                if(!f.isDirectory()) {
                    f.mkdirs();
                }
                ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
                try {
                    ZipEntry ze = null;
                    int total = 0;
                    while ((ze = zin.getNextEntry()) != null) {
                        String path = location + ze.getName();
                        Log.d(TAG,"unzipping file "+path);

                        progressVal += 5;
                        if (progressVal > 80) progressVal = 80;
                        publishProgress(progressVal);

                        if (path.endsWith(".wad") == true)
                            returnFilename = path;  // save for return if wad file

                        // what about mod files?! XXX
                        // mod file will have lots of stuff in it but no wad files
                        // thus, if we don't find a wad file, its a mod
                        // move the whole mod sub-directory to mods dir in the game engine

                        if (ze.isDirectory()) {
                            File unzipFile = new File(path);
                            if(!unzipFile.isDirectory()) {
                                unzipFile.mkdirs();
                            }
                        }
                        else {
                            FileOutputStream fout = new FileOutputStream(path, false);
                            try {
                                for (int c = zin.read(); c != -1; c = zin.read()) {

                                    fout.write(c);

                                    total+=1;

                                    if ((total % 100000) == 0) {
                                        progressVal += 1;
                                        if (progressVal > 80) progressVal = 80;
                                        publishProgress(progressVal);
                                    }

                                }
                                zin.closeEntry();
                            }
                            finally {
                                fout.close();
                            }
                        }
                        Log.d(TAG,"unzip finished file");
                    }
                    Log.d(TAG,"unzip finished");
                }
                finally {
                    zin.close();
                }
            }
            catch (Exception e) {
                Log.e(TAG, "Unzip exception", e);
            }


            return returnFilename;

        } // unzipFile()

        public String unzipFileBlock(int progressVal, String zipFile, String location)
        {
            int size;
            int BUFFER_SIZE = 8192;
            byte[] buffer = new byte[BUFFER_SIZE];
            String returnFilename = new String("");

            // how do we know if we are unzipping a mod zip file?
            // if so, we need to return the name of the directory we unzip into XXX

            try {
                if ( !location.endsWith(File.separator) ) {
                    location += File.separator;
                }
                File f = new File(location);
                if(!f.isDirectory()) {
                    f.mkdirs();
                }
                ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));
                try {
                    int total = 0;
                    ZipEntry ze = null;
                    while ((ze = zin.getNextEntry()) != null) {
                        String path = location + ze.getName();
                        File unzipFile = new File(path);

                        if (path.endsWith(".wad") == true)
                            returnFilename = path;  // save for return if wad file

                        progressVal += 5;
                        if (progressVal > 80) progressVal = 80;
                        publishProgress(progressVal);

                        if (ze.isDirectory()) {
                            if(!unzipFile.isDirectory()) {
                                unzipFile.mkdirs();
                            }
                        } else {
                            // check for and create parent directories if they don't exist
                            File parentDir = unzipFile.getParentFile();
                            if ( null != parentDir ) {
                                if ( !parentDir.isDirectory() ) {
                                    parentDir.mkdirs();
                                }
                            }

                            // unzip the file
                            FileOutputStream out = new FileOutputStream(unzipFile, false);
                            BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                            try {
                                while ( (size = zin.read(buffer, 0, BUFFER_SIZE)) != -1 ) {
                                    fout.write(buffer, 0, size);

                                    total += size;

                                    if ((total % 100000) == 0) {
                                        progressVal += 1;
                                        if (progressVal > 80) progressVal = 80;
                                        publishProgress(progressVal);
                                    }
                                }

                                zin.closeEntry();
                            }
                            finally {
                                fout.flush();
                                fout.close();
                            }
                        }
                    }
                }
                finally {
                    zin.close();
                }

            }
            catch (Exception e) {
                Log.e(TAG, "Unzip exception", e);
            }

            // if there was no wad file detected, then we assume its a mod;
            // set mod type ? XXX
            // return the exploded directory?
            // skipping mod files for now . . .

            return returnFilename;

        } // unzipFileBlock()

    } // DownloadFileFromURL private class

} // MainActivity

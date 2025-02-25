package net.nullsum.freedoom;


import android.app.Activity;
import android.app.Dialog;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class ModSelectDialog {
    private final Dialog dialog;
    private String basePath;
    private String extraPath = "";
    private boolean PrBoomMode;
    private ArrayList<String> filesArray = new ArrayList<>();
    private ArrayList<String> selectedArray = new ArrayList<>();
    private Activity activity;
    private TextView resultTextView;
    private TextView infoTextView;

    private ModsListAdapter listAdapter;

    ModSelectDialog(Activity act, String path, boolean prboomMode) {
        basePath = path;
        activity = act;
        PrBoomMode = prboomMode;

        dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_select_mods_wads);
        dialog.setTitle("Touch Control Sensitivity Settings");
        dialog.setCancelable(true);

        dialog.setOnKeyListener((arg0, keyCode, event) -> {
            // TODO Auto-generated method stub
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (extraPath.isEmpty() || !extraPath.contains("/"))
                    return false;
                else {
                    extraPath = extraPath.substring(0, extraPath.lastIndexOf("/"));
                    populateList(extraPath);
                    return true;
                }
            }
            return false;
        });

        resultTextView = dialog.findViewById(R.id.result_textView);
        infoTextView = dialog.findViewById(R.id.info_textView);

        ListView listView = dialog.findViewById(R.id.listview);
        listAdapter = new ModsListAdapter(activity);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {

            if (filesArray.get(position).startsWith("/")) {
                populateList(extraPath + filesArray.get(position));
            } else //select/deselect
            {
                boolean removed = false;
                for (Iterator<String> iter = selectedArray.listIterator(); iter.hasNext(); ) {
                    String s = iter.next();
                    if (s.contentEquals(extraPath + "/" + filesArray.get(position))) {
                        iter.remove();
                        removed = true;
                    }
                }

                if (!removed)
                    selectedArray.add(extraPath + "/" + filesArray.get(position));

                //Log.d("TEST", "list size = " + selectedArray.size());

                listAdapter.notifyDataSetChanged();
                resultTextView.setText(getResult());

            }
        });

        //Add folders on long press
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            if (filesArray.get(position).startsWith("/")) {
                boolean removed = false;
                String name = filesArray.get(position).substring(1);
                for (Iterator<String> iter = selectedArray.listIterator(); iter.hasNext(); ) {
                    String s = iter.next();
                    if (s.contentEquals(extraPath + "/" + name)) {
                        iter.remove();
                        removed = true;
                    }
                }

                if (!removed)
                    selectedArray.add(extraPath + "/" + name);

                //Log.d("TEST", "list size = " + selectedArray.size());

                listAdapter.notifyDataSetChanged();
                resultTextView.setText(getResult());
                return true;
            }
            return false;
        });

        Button wads_button = dialog.findViewById(R.id.wads_button);
        wads_button.setOnClickListener(v -> populateList("wads"));

        Button mods_button = dialog.findViewById(R.id.mods_button);
        mods_button.setOnClickListener(v -> populateList("mods"));

        Button ok_button = dialog.findViewById(R.id.ok_button);
        ok_button.setOnClickListener(v -> {
            dialog.dismiss();
            resultResult(getResult());
        });

        populateList("wads");

        dialog.show();
    }

    public void resultResult(String result) {
    }

    public String getResult() {
        StringBuilder result = new StringBuilder();

        if (PrBoomMode && selectedArray.size() > 0)
            result = new StringBuilder("-file");

        for (int n = 0; n < selectedArray.size(); n++) {
            if (PrBoomMode)
                result.append(" ").append(selectedArray.get(n));
            else {
                if ((selectedArray.get(n).endsWith(".deh")) || (selectedArray.get(n).endsWith(".bex")))
                    result.append("-deh ").append(selectedArray.get(n)).append(" ");
                else
                    result.append("-file ").append(selectedArray.get(n)).append(" ");
            }
        }

        return result.toString();
    }


    private void populateList(String path) {
        extraPath = path;
        dialog.setTitle(extraPath);
        String wad_dir = basePath + "/" + path;
        File[] files = new File(wad_dir).listFiles();
        filesArray.clear();
        if (files != null)
            for (File f : files) {
                if (!f.isDirectory()) {
                    String file = f.getName().toLowerCase();
                    if (file.endsWith(".wad") || file.endsWith(".pk3") || file.endsWith(".pk7") || file.endsWith(".deh")) {
                        filesArray.add(f.getName());
                    }
                } else //Now also do directories
                {
                    filesArray.add("/" + f.getName());
                }
            }

        Collections.sort(filesArray);

        if (filesArray.size() == 0)
            infoTextView.setText("Please copy addon wad/mods to here: \"" + basePath + "/" + path + "\"");
        else
            infoTextView.setText("");

        listAdapter.notifyDataSetChanged();
        resultTextView.setText(getResult());
    }

    class ModsListAdapter extends BaseAdapter {

        public ModsListAdapter(Activity context) {
        }

        public void add(String string) {
        }

        public int getCount() {
            return filesArray.size();
        }

        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup list) {

            View view;

            if (convertView == null) {
                view = activity.getLayoutInflater().inflate(R.layout.listview_item_mods_wads, null);
            } else {
                view = convertView;
            }

            boolean selected = false;
            for (String s : selectedArray) {
                if (s.contentEquals(extraPath + "/" + filesArray.get(position))) {
                    selected = true;
                }
            }

            if (selected) {
                view.setBackgroundResource(R.drawable.layout_sel_background);
            } else {
                view.setBackgroundResource(0);
            }

            TextView title = view.findViewById(R.id.name_textview);

            title.setText(filesArray.get(position));
            return view;
        }
    }
}

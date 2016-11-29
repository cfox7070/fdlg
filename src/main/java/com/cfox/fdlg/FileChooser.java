package com.cfox.fdlg;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;


public class FileChooser
{
    private static final String PARENT_DIR = "..";
	public enum Mode
	{open,create}

	private final File extStorage;
    private ListView list;
    private Dialog dialog;
    private File currentPath;
	private Mode mode=Mode.open;
	private boolean showHidden=false;
	private String extension = null;

	EditText fname;
	TextView fext;

	private boolean maycreate=true;

	public FileChooser setMode(Mode mode)
	{
		this.mode = mode;
		if (mode == Mode.create)
		{
			fname.setVisibility(View.VISIBLE);
			fext.setVisibility(View.VISIBLE);
		}else{
			fname.setVisibility(View.GONE);
			fext.setVisibility(View.GONE);

		}
		return this;
	}

	public FileChooser setFileName(String name)
	{
        fname.setText(name);
		return this;
	}
    // filter on file extension
    
    public FileChooser setExtension(String extension)
	{
        this.extension = (extension == null) ? null :
			extension.toLowerCase();
		TextView txext=(TextView) dialog.findViewById(R.id.fext);
		if(this.extension==null || this.extension.equals("")){
			txext.setText("*.*");
		}else{
			txext.setText("*."+this.extension);
		}
		return this;
    }

    // file selection event handling
    public interface FileSelectedListener
	{
        void fileSelected(File file);
    }
    public FileChooser setFileListener(FileSelectedListener fileListener)
	{
        this.fileListener = fileListener;
        return this;
    }
    private FileSelectedListener fileListener;

    public FileChooser(Context context)
	{
		//todo: layouts template
        dialog = new Dialog(context,R.style.FileDialogTheme);
		dialog.setContentView(R.layout.filechooser);
		fname= (EditText) dialog.findViewById(R.id.fname);
		fext= (TextView) dialog.findViewById(R.id.fext);
        //list = new ListView(activity);
		list = (ListView)dialog.findViewById(R.id.flist);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override public void onItemClick(AdapterView<?> parent, View view, int which, long id)
				{
					File chosenFile = (File) list.getItemAtPosition(which);
					if(chosenFile==null){
						chosenFile=currentPath.getParentFile();
					}
                    if (!chosenFile.isDirectory()) {
                        fname.setText(chosenFile.getName());
                    }
					//File chosenFile = getChosenFile(fileChosen);
					if (chosenFile.isDirectory())
					{
						refresh(chosenFile);
					}

				}
			});

       // dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

		extStorage=Environment.getExternalStorageDirectory();
        refresh(extStorage);
        setExtension(null);
		setMode(Mode.open);

		Button bcancel =(Button) dialog.findViewById(R.id.fcancel);
		bcancel.setOnClickListener(new View.OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					dialog.dismiss();
				}		
			});

		Button bok =(Button) dialog.findViewById(R.id.fok);
		bok.setOnClickListener(new View.OnClickListener(){

				@Override
                public void onClick(View p1) {
                    File chosenFile = null;
                    if (mode == Mode.open) {
                        int pos = list.getCheckedItemPosition();
                        if (pos == ListView.INVALID_POSITION)
                            return;
                        chosenFile = (File) list.getItemAtPosition(pos);
                        if (chosenFile == null) {
                            return;
                        }
						if (fileListener != null) {
							fileListener.fileSelected(chosenFile);
						}
						dialog.dismiss();
					} else if (mode == Mode.create) {
                        String sfname = fname.getText().toString();
                        if (extension != null) {
                            if (!sfname.endsWith(extension)) {
                                sfname += "."+extension;
                            }
                        }
                        final String newname = currentPath.getPath() + File.separator+sfname;
                        if(newname==null || newname.equals(""))
                            return;
                        chosenFile = new File(newname);
                        try {
							if (chosenFile.exists()) {
								new AlertDialog.Builder(dialog.getContext())
										.setTitle(R.string.overwrite)
										.setMessage(R.string.shureoverwrite)
										.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface adialog, int which) {
                                                File newFile=new File(newname);
                                                newFile.delete();
                                                try {
                                                    newFile.createNewFile();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                                if (fileListener != null) {
                                                    fileListener.fileSelected(newFile);
                                                }
                                                adialog.dismiss();
                                                dialog.dismiss();
											}
										})
										.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
											public void onClick(DialogInterface adialog, int which) {
												adialog.dismiss();
											}
										})
										.setIcon(android.R.drawable.ic_dialog_alert)
										.setCancelable(false)
										.show();
							}else{
                                chosenFile.createNewFile();
                                if (fileListener != null) {
                                    fileListener.fileSelected(chosenFile);
                                }
                                dialog.dismiss();
                            }

						} catch (IOException x) {
                            System.err.format("createFile error: %s%n", x.getMessage());
                        }
                  }
                }
        });

        fname = (EditText) dialog.findViewById(R.id.fname);
        fext = (TextView) dialog.findViewById(R.id.fext);
    }

    public void showDialog()
	{
        dialog.show();
    }

    /**
     * Sort, filter and display the files for the given path.
     */
    private void refresh(File path)
	{
        this.currentPath = path;
        if (path.exists())
		{
            File[] files = path.listFiles(new FileFilter() {
					@Override public boolean accept(File file)
					{
						if (!showHidden && file.getName().charAt(0) == '.')
							return false;
						if (!file.isDirectory() && extension != null)
							return file.getName().toLowerCase().endsWith(extension);
						return (file.canRead());//TODO:if can't read???
					}
				});
            int i = 0;

            if (!(path.getParentFile() == null || path==extStorage))
			{
				files=Arrays.copyOf(files,files.length+1);
            }

			Comparator<File> cmp=new Comparator<File>(){

				@Override
				public int compare(File p1, File p2)
				{
					if(p1==null){
						return -1;
					}if (p1.isDirectory() && !p2.isDirectory())
					{
						return -1;
					}
					else if (!p1.isDirectory() && p2.isDirectory())
					{
						return 1;
					}
					else
					{
						return p1.getName().toLowerCase().
							compareTo(p2.getName().toLowerCase());
					}
				}			
			};	

            Arrays.sort(files, cmp);
            dialog.setTitle(currentPath.getPath());
			list.setAdapter(new MySimpleArrayAdapter(dialog.getContext(),files));
        }
    }

	static class ViewHolder {
		public TextView text;
		public ImageView image;
	}

	private class MySimpleArrayAdapter extends ArrayAdapter<File> {
		private final Context context;
		private final File[] values;

		public MySimpleArrayAdapter(Context context, File[] values) {
			super(context, -1, values);
			this.context = context;
			this.values = values;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView=convertView;
			ViewHolder viewHolder=null;
			if(rowView==null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				rowView = inflater.inflate(R.layout.flisttext, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.text = (TextView) rowView.findViewById(R.id.f_txt);
				viewHolder.image = (ImageView) rowView.findViewById(R.id.f_img);
				rowView.setTag(viewHolder);
			}
			viewHolder=(ViewHolder) rowView.getTag();
			if(values[position]==null){
				viewHolder.text.setText("..");
				viewHolder.image.setImageResource(R.drawable.bg);
			}else if (values[position].isDirectory()) {
				viewHolder.image.setImageResource(R.drawable.bg);
				viewHolder.text.setText(values[position].getName());
			} else {
				viewHolder.image.setImageResource(R.drawable.fl);
				viewHolder.text.setText(values[position].getName());
			}
			return rowView;
		}
	}
}

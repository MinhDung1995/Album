package com.example.galleryds;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

public class AlbumManager {

	// Singleton 
	protected static AlbumManager _instance = null;
	
    // Cho việc thao tác trên mục album ảnh
	private GridView _gridViewAlbum;
    protected AlbumAdapter _albumAdapter;
    protected ArrayList<String> _albumList;
    protected HashMap<String, ArrayList<DataHolder>> _albumData;
    protected ImageAdapter _selectedAlbumImagesAdapter;
    
    // Khởi tạo
    protected AlbumManager(Context context, GridView gridViewAlbum)
    {
    	_gridViewAlbum = gridViewAlbum;
    	_selectedAlbumImagesAdapter = new ImageAdapter(context, new ArrayList<DataHolder>());
    }
    
    public void onRotateScreen(Context context, GridView gridViewAlbum) {
    	_gridViewAlbum = gridViewAlbum;
    	_selectedAlbumImagesAdapter = new ImageAdapter(context, new ArrayList<DataHolder>());
    }
    
    // Khởi tạo với tham số
    public static AlbumManager getInstance(Context context, GridView gridView)
    {
    	if (_instance == null)
    		_instance = new AlbumManager(context, gridView);
    	
    	return _instance;
    }
    
    public static AlbumManager getInstance()
    {
    	return _instance;
    }
    
    // Lấy adapter cho album được chọn
    public ImageAdapter getSelectedAlbumAdapter(String albumName)
    {
    	ArrayList<DataHolder> dataHolder = this.getAlbumData(albumName);
    	_selectedAlbumImagesAdapter.updateData(dataHolder);	
    	
    	return _selectedAlbumImagesAdapter;
    }
    
    // Lấy GridView của Album
    public GridView getGridViewAlbum() 
    {
		return _gridViewAlbum;
	}
    
    // Lấy dữ liệu của Album
    public ArrayList<DataHolder> getAlbumData(String albumName) 
    {
    	if (_albumData.containsKey(albumName))
    		return _albumData.get(albumName);
    	
    	return null;
	}

    // Lấy danh sách các album
    public ArrayList<String> getAlbumList()
    {
    	return _albumList; 
    }
    
    // Kiểm tra có tồn tại album đó không 
    public boolean containsAlbum(String albumName)
    {
    	return _albumData.containsKey(albumName);
    }
    
    // Lấy context
    protected Context getContext()
    {
    	return _selectedAlbumImagesAdapter.getContext();
    }

    // Nạp các album lên
    public void loadAlbums()
    {
        _albumList = AlbumManager.getAlbumPaths(getContext());

        if (_albumList == null)
        	_albumList = new ArrayList<String>();
        
        // Gắn adapter vào gridview
        _albumAdapter = new AlbumAdapter(getContext(), _albumList);
        getGridViewAlbum().setAdapter(_albumAdapter);
        
        // Tạo dữ liệu cho các album
        _albumData = new HashMap<String, ArrayList<DataHolder>>();
        
        for (int i = 0; i < _albumList.size(); i++)
        	_albumData.put(_albumList.get(i), new ArrayList<DataHolder>());
    }
    
    // Xóa dữ liệu ảnh trong album cũ
    public void removeImageDataFromAlbum(DataHolder data)
    {
    	File f = new File(data.getFilePath());
    	String parent = f.getParentFile().getName();

		if (this.containsAlbum(parent))
			this.getAlbumData(parent).remove(data);
    }
    
    // Bỏ ảnh khỏi album
    public void removeImageFromAlbum(DataHolder data, String album)
    {   
    	ArrayList<DataHolder> albumImages = this.getAlbumData(album);
    	
    	if (albumImages == null)
    		return;
    	
    	// Xóa dữ liệu khỏi album
    	albumImages.remove(data);
    	
        // Di chuyển file
    	File f = new File(data.getFilePath());
        String fName = f.getName();       
        ImageSupporter.moveFile(f.getParentFile().getAbsolutePath(), fName, ImageSupporter.DEFAULT_PICTUREPATH);
        
        // Cập nhật thông tin file mới cho dữ liệu
        data.setFilePath(ImageSupporter.DEFAULT_PICTUREPATH + File.separator + fName);
    }
    
    // Thêm ảnh vào album
    public void addImageToAlbum(DataHolder data, String albumName)
    {	
    	File f = new File(data.getFilePath());
    	File parent = f.getParentFile();
    	
    	// Kiểm tra xem ảnh có trong album chưa
    	if (albumName.equals(parent.getName()))
    		return;
		
		// Xóa dữ liệu khỏi album cũ nếu có
		this.removeImageDataFromAlbum(data);
		
		// Di chuyển file
		ImageSupporter.moveFile(parent.getAbsolutePath(), f.getName(), ImageSupporter.DEFAULT_PICTUREPATH + File.separator + albumName);
		
		// Cập nhật thông tin file
		String newPath = ImageSupporter.DEFAULT_PICTUREPATH + File.separator + albumName + File.separator + f.getName();
		data.setFilePath(newPath);
		
		// Thêm vào album
    	this.getAlbumData(albumName).add(data);
    }
    
    // Tạo mới Album
    public boolean createAlbum(String name) 
    {   	
    	try
    	{
	    	// Tập tin chứa ảnh mặc định của thiết bị
	        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	
	        // Kiểm tra có tạo thư mục (Album) thành công không
	        if (ImageSupporter.createFolder(path.getAbsolutePath(), name))
	        {
	           
	            ArrayList<String> data = new ArrayList<String>();
	            data.add(name);
	            
	            // Cập nhật dữ liệu tập tin 
	            AlbumManager.addNewAlbumPaths(getContext(), data);
	            
	            // Cập nhật adapter và giao diện
	            _albumAdapter.add(name);
	            _albumData.put(name, new ArrayList<DataHolder>());
	            
	            return true;
	        } 
	        
	        return false;
    	}
    	catch (Exception e)
    	{
    		return false;
    	}
    }

    // Đổi tên 1 Album
    public boolean renameAlbum(String oldName, String newName) 
    {
    	try
    	{
	    	// Tập tin chứa ảnh mặc định của thiết bị 
	        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	        
	        // Thư mục album được chọn
	        File album = new File(path.getAbsolutePath() + File.separator + oldName);
	
	        // Cập nhật trên cơ sở dữ liệu 
	        renameAlbumDataOnFileByName(oldName, newName);
	
	    	// Cập nhật trên adapter
	        _albumAdapter.remove(oldName);
	        _albumAdapter.add(newName);
	
	        // Đổi tên thư mục album
	        ImageSupporter.renameFile(album, newName);
	        
	        return true;
    	}
    	catch (Exception e)
    	{
    		return false;
    	}
    }

    // Xóa toàn bộ ảnh trong album
    public boolean deleteWholeAlbum(String name) 
    {
    	try
    	{
	    	// Tập tin chứa ảnh mặc định của thiết bị 
	        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	
	    	// Thực hiện xóa album và toàn bộ ảnh trong album
	        ImageSupporter.deleteWholeFolder(path.getAbsolutePath(), name);
	        
	        return true;
	    }
        catch (Exception e)
    	{
    		return false;
    	}
    }

    // Xóa album và chuyển ảnh sang chỗ khác
    public boolean deleteAlbum(String name) 
    {
    	try
    	{
	    	// Lấy thư mục album
	        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	        File folder = new File(path.getAbsolutePath() + File.separator + name);
	
	        // Kiểm tra có tồn tại thư mục đó không
	        if (folder.exists())
	        {
	            File[] files = folder.listFiles();
	
	            // Nếu có tập tin bên trong thực hiện chuyển tập tin ra chỗ khác
	            if (files != null)
	                for (File f : files)
	                    ImageSupporter.moveFile(f.getParent(), f.getName(), path.getAbsolutePath());
	
	            // Thực hiện xóa thư mục
	            folder.delete();	
	            
	            // Xóa trên cơ sỡ dữ liệu
	            removeAlbumDataOnFileByName(name);
	            
	            // Xóa trên adapter
		        _albumAdapter.remove(name);
		        
		        return true;
	        }
	        
	        return false;
    	}
    	catch (Exception e)
    	{
    		return false;
    	}
    }
    
    // Xóa trên cơ sở dữ liệu
    protected void removeAlbumDataOnFileByName(String name)
    {
    	// Lấy đường dẫn các album
    	ArrayList<String> data = AlbumManager.getAlbumPaths(getContext());
    	
    	// Tìm và xóa album
    	int i =0;
        for(i = 0; i < data.size(); i++)
            if (data.get(i).equals(name)) 
        		break;
        
        data.remove(i);
      
        // Tìm kiếm và đổi tên mới
        AlbumManager.saveAlbumPaths(getContext(), data);
    }
    
    // Cập nhật trên cơ sở dữ liệu
    protected void renameAlbumDataOnFileByName(String oldName, String newName)
    {
	    // Lấy đường dẫn các album
	    ArrayList<String> data = AlbumManager.getAlbumPaths(getContext());
	    
	    // Tìm kiếm và đổi tên mới
	    int i =0;
        for(i = 0; i < data.size(); i++)
            if (data.get(i).equals(oldName)) 
        		break;
        
	    data.set(i, newName);
	    
	    // Lưu dữ liệu trên tập tin dữ liệu
	    AlbumManager.saveAlbumPaths(getContext(), data);
    }
 
    // Bật chế độ chọn hình ảnh
    public void turnOffSelectionAlbumMode()
    {  
        int count = _albumAdapter.getCount();

        for (int i = 0; i < count; i++) {
            View view = ImageSupporter.getViewByPosition(i, _gridViewAlbum);

            AlbumViewHolder holder = (AlbumViewHolder) view.getTag();

            holder.checkbox.setVisibility(View.INVISIBLE);
            holder.checkbox.setChecked(false);
        }
    }

    // Bật chế độ chọn hình ảnh
    public void turnOnSelectionAlbumMode() {
        int count = _albumAdapter.getCount();

        for (int i = 0; i < count; i++) {
            View view = ImageSupporter.getViewByPosition(i, _gridViewAlbum);

            AlbumViewHolder holder = (AlbumViewHolder) view.getTag();

            holder.checkbox.setVisibility(View.VISIBLE);
            holder.checkbox.setChecked(false);
        }
    }
    
    // Xóa album đã được chọn
    public void deleteSelectedAlbums() 
    {
        int count = _albumAdapter.getCount();

        for (int i = count - 1; i >= 0; i--)
        {
            View view = ImageSupporter.getViewByPosition(i, _gridViewAlbum);

            AlbumViewHolder holder = (AlbumViewHolder) view.getTag();

            if (holder.checkbox.isChecked() == true)
            	this.deleteAlbum(holder.textview.getText().toString());
        }
    } 

    // Áp dụng insert sort với độ phức tạp O(n)
    // Tiện cho việc tìm kiếm nhị phân
    public void insertImageDataToAlbum(String albumName, DataHolder data)
    {    	
    	ImageSupporter.binaryInsertByLastModifiedDate(this.getAlbumData(albumName), data);
    }
    
	// Lấy thông tin ảnh ưa thích
    protected static ArrayList<String> getAlbumPaths(Context context)
	{
		ArrayList<String> result = null;
		
	    try 
	    {
	        InputStream inputStream = context.openFileInput("GalleryDS_Album.txt");
	        result = new ArrayList<String>();
	        
	        if ( inputStream != null )
	        {
	            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
	            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
	            String receiveString = "";

	            while ((receiveString = bufferedReader.readLine()) != null) 
	                result.add(receiveString);

	            inputStream.close();
	        }
	        
		    return result;
	    }
	    catch (FileNotFoundException e) 
	    {
	    	Log.e("GalleryDS_Album", "File not found: " + e.toString());
	        return null;
	    } catch (IOException e) 
	    {
	    	Log.e("GalleryDS_Album", "Can not read file: " + e.toString());
	        return null;
	    }
	}

	// Lưu thông tin ảnh ưa thích
	protected static boolean saveAlbumPaths(Context context, ArrayList<String> data)
	{
		try
		{
			FileOutputStream fos = context.openFileOutput("GalleryDS_Album.txt", Context.MODE_PRIVATE);
			
			if (data != null)
				for (int i = 0; i < data.size(); i++) 
					fos.write((data.get(i) + "\n").getBytes());
			
			fos.close();
			
			return true;
		}
		catch (Exception e)
		{
			Log.e("GalleryDS_Album", e.getMessage());
	        return false;
		}
	}
		
	// Lưu thêm thông tin ảnh ưa thích
	protected static boolean addNewAlbumPaths(Context context, ArrayList<String> data)
	{
		try
		{
			FileOutputStream fos = context.openFileOutput("GalleryDS_Album.txt", Context.MODE_PRIVATE | Context.MODE_APPEND);
			
			if (data != null)
				for (int i = 0; i < data.size(); i++) 
					fos.write((data.get(i) + "\n").getBytes());
			
			fos.close();
			
			return true;
		}
		catch (Exception e)
		{
			Log.e("GalleryDS_Album", e.getMessage());
	        return false;
		}
	}
}

package com.example.t_gallery;

import java.util.ArrayList;
import java.util.Random;

class ImageCell {
	ImageCell(long aId, int aWidth, int aHeight, int aPosition){
		id = aId;
		position = aPosition;
	    inWidth = aWidth;
	    inHeight = aHeight;
	    yRatio = (float)inHeight / (float)inWidth;
	}
	
	public void multiplyXGravity(float factor){
		xGravity *= factor;
	}
	
	public void applyWidth(int totalWidth){
		outWidth = (int)(xGravity * totalWidth);
		outHeight = (int)(yRatio * outWidth);
	}
	
	public void adjustXGravity(float SiblingYRatio){
		xGravity = SiblingYRatio/(yRatio + SiblingYRatio);
	}
	
	
	public long id = 0;
	public int position = 0;
	
	public int inWidth = 0;
	public int inHeight = 0;
	
	public int outWidth = 0;
	public int outHeight = 0;
	
	public int outX = 0;
	public int outY = 0;
	
	/*For calculate*/
	public float xGravity = 1.0f;
	public float yRatio = 0.0f;
}


class ImageLineGroup {

	ImageLineGroup(){
		imageList = new ArrayList<ImageCell>();
	}

	public void addImage(long id, int width, int height, int position){
		ImageCell image = new ImageCell(id, width, height, position);
		imageList.add(image);
	}
	
	public void addImage(ImageCell image){
		imageList.add(image);
	}
	
	public int getImageNum(){
		return imageList.size();
	}
	
	public ImageCell getImage(int i){
		return imageList.get(i);
	}
	
	public int getHeight(){
		return height;
	}
	
	protected int height;
	public ArrayList<ImageCell> imageList;
}

class ImageSingleLineGroup extends ImageLineGroup{
	private int totalWidth = 1080;
	private static final int MAX_HEIGHT = 540;
	private static final int MIN_HEIGHT = 180;
	
	ImageSingleLineGroup(int containerWidth){
		totalWidth = containerWidth;
	}
	
	
	private void layout(){
		int maxContentWidth = totalWidth;
		int contentWidth = 0;
		
		/*First Round, to resize all picture as high as MAX_HEIGHT*/
		for (int i=0; i<imageList.size(); i++){
			ImageCell image = imageList.get(i);
			image.outHeight = MAX_HEIGHT;
			image.outWidth = (image.inWidth*image.outHeight)/image.inHeight;
			
			contentWidth += image.outWidth;
		}
		
		if (contentWidth > maxContentWidth){
			for (int i=0; i<imageList.size(); i++){
				ImageCell image = imageList.get(i);
				
				image.outHeight = (image.outHeight*maxContentWidth)/contentWidth;
				image.outWidth = (image.outWidth*maxContentWidth)/contentWidth;
				
				height = image.outHeight + 2*Config.THUMBNAIL_PADDING;
			}
		}
		
		contentWidth = 0;
		
		for (int i=0; i<imageList.size(); i++){
			ImageCell image = imageList.get(i);
			
			image.outX = contentWidth;
			image.outY = 0;
			
			contentWidth += image.outWidth + 2*Config.THUMBNAIL_PADDING;
		}
		
	}
	
	public boolean properForMoreImage(){
		if (imageList.size() >= 4 || height<=MIN_HEIGHT){
			return false;
		}
		return true;
	}
	
	public boolean needMoreImage(){
		if (imageList.isEmpty()){
			return true;
		}
		else if (imageList.size() >= 4){
			layout();
			return false; //Consider as "full" if 4 pictures in one line...
		}
		
		layout();
		
		int contentWidth = 0;
		
		for (int i=0; i<imageList.size(); i++){
			ImageCell image = imageList.get(i);			
			contentWidth += (image.outWidth + 2*Config.THUMBNAIL_PADDING);
		}
		
		if (contentWidth < (totalWidth-30)){
			return true;
		}
		else {
			return false;
		}

	}	
}

class ImageProcessBuffer{	
	ImageProcessBuffer(int length){
		buffer = new ArrayList<ImageCell>();
		maxLength = length;
	}
	
	public void add(ImageCell cell){
		buffer.add(cell);
	}
	
	public ImageCell remove(int index){
		return buffer.remove(index);
	}
	
	public boolean isFull(){
		if (buffer.size() == maxLength){
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean isEmpty(){
		return buffer.isEmpty();
	}
	
	public ArrayList<ImageCell> shed(int length){
		if (length > buffer.size()){
			length = buffer.size();
		}
		
		ArrayList<ImageCell> result = new ArrayList<ImageCell>();
		
		for (int i=0; i<length; i++){
			result.add(buffer.remove(0));
		}
		
		return result;
	}
	
	public ArrayList<ImageCell> buffer;
	private int maxLength = 0;
}

abstract class ImageRichLinePattern {
	/*Return the number of items matching the pattern*/
	abstract int match(ArrayList<ImageCell> images);
	
	/*Just return how many items the pattern applies*/
	abstract int imageCount();
	
	/*Return the height of line*/
	abstract int layout(ArrayList<ImageCell> images, int totalWidth);
	
	private void fillSubDet(float in[][],float out[][], int removeX, int removeY, int lev){
		
		for (int i=0; i<(lev-1); i++){
			for (int j=0; j<(lev-1); j++){
				if (i>=removeX && j>=removeY){
					out[i][j] = in[i+1][j+1];
				}
				else if (i>=removeX){
					out[i][j] = in[i+1][j];
				}
				else if (j>=removeY){
					out[i][j] = in[i][j+1];
				}
				else {
					out[i][j] = in[i][j];
				}
			}
		}
	}
	
	private void fillValueDet(float in[][], float out[][], int replaceY, int lev){
		for (int i=0; i<lev; i++){
			for (int j=0; j<lev; j++){
				if (j==replaceY){
					out[i][j] = in[i][lev];
				}
				else {
					out[i][j] = in[i][j];
				}
			}
		}
	}
	
	private float calcDet(float mat[][], int lev){
		if (lev == 2){
			return ((mat[0][0]*mat[1][1])-(mat[0][1]*mat[1][0]));
		}
		else {
			float result = 0.0f;
			float tempDet[][] = new float[lev-1][lev-1];
			
			for (int i=0; i<lev; i++){
				fillSubDet(mat, tempDet, 0, i, lev);
				
				float subDetValue = calcDet(tempDet, lev-1);
				if (i%2 == 0){
					result += mat[0][i] * subDetValue;
				}
				else {
					result -= mat[0][i] * subDetValue;
				}
			}
			return result;
		}
	}
	
	protected void calcMatrix(float mat[][], float result[], int lev){
        float tempValueDet[][] = new float[lev][lev];
        float baseDetValue = calcDet(mat, lev);
        
        for (int i=0; i<lev; i++){
        	fillValueDet(mat, tempValueDet, i, lev);
        	
        	result[i] = calcDet(tempValueDet, lev) / baseDetValue;
        }
	
	}
}

class ImageRichLinePatternCollection {
	private static final int PATTERN_NUM = 17;
	
	private static final int IMAGE_PANORAMA = 0x00000001;
	private static final int IMAGE_LANDSCAPE = 0x00000002;
	private static final int IMAGE_SQUARE = 0x00000004;
	private static final int IMAGE_PORTRAIT = 0x00000008;
	private static final int IMAGE_SLIM = 0x00000010;
	
	public int getImageType(ImageCell image){
		if (image.yRatio <= 0.4f){
			return IMAGE_PANORAMA;
		}
		else if (image.yRatio <= 0.85f){
			return IMAGE_LANDSCAPE;
		}
		else if (image.yRatio <= 1.15f){
			return IMAGE_SQUARE;
		}
		else if (image.yRatio <= 2.5f){
			return IMAGE_PORTRAIT;
		}
		else {
			return IMAGE_SLIM;
		}
	}
	
	public boolean isImageListPortrait(ArrayList<ImageCell> images, int aTypes[], int aNum[], int lev){
		//aTypes.length == aNum.length && lev <= images.size()
		boolean res = true;

		for(int i = 0; i < lev; i++) {
			int type = getImageType(images.get(i));

			for(int j = 0; j < aTypes.length; j++) {
				if(aNum[j] > 0 && (aTypes[j] & type) != 0) {
					aNum[j]--;
					break;
				}
			}
		}

		for(int i = 0; i< aNum.length; i++) {
			if(aNum[i] != 0) {
				res = false;
				break;
			}
		}
		
		return res;
	}
	
	public void adjustImageList(ArrayList<ImageCell> images,
			ArrayList<ImageCell> adjustImages, int baseIndex[]) {
		// images.size() = baseIndex.length
		adjustImages.add(images.get(0));
		baseIndex[0] = 0;

		for (int i = 1; i < images.size(); i++) {
			int j = 0;
			for (; j < adjustImages.size(); j++) {
				if (images.get(i).yRatio > adjustImages.get(j).yRatio) {
					for (int k = adjustImages.size() - 1; k >= j; k--) {
						baseIndex[k + 1] = baseIndex[k];
					}
					break;
				}
			}
			adjustImages.add(j, images.get(i));
			baseIndex[j] = i;
		}
	}
	
	public void restoreImageList(ArrayList<ImageCell> images, ArrayList<ImageCell> adjustImages, int baseIndex[]) {
		//adjustImages.size() = baseIndex.length
    	for(int i = 0; i < adjustImages.size(); i++) {
    		images.get(baseIndex[i]).outWidth = adjustImages.get(i).outWidth;
    		images.get(baseIndex[i]).outHeight = adjustImages.get(i).outHeight;
    		images.get(baseIndex[i]).outX = adjustImages.get(i).outX;
    		images.get(baseIndex[i]).outY = adjustImages.get(i).outY;
      	}
	}
	
	ImageRichLinePatternCollection(){
		patterns = new ImageRichLinePattern[PATTERN_NUM];
		availablePatterns = new ArrayList<Integer>();
		aPatternMatchSum = new int[PATTERN_NUM];
		
		for(int i = 0; i < PATTERN_NUM; i++) {
			aPatternMatchSum[i] = 0;
		}
		
		patterns[0] = new ImageRichLinePattern(){
						
			public int imageCount(){
				return 5;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 5){
					return -1;
				}
				
				for (int i=0; i<5; i++){
					int type = getImageType(images.get(i));
					
					if (IMAGE_LANDSCAPE == type || IMAGE_PANORAMA == type){
						return -1;
					}
				}
				return 5;
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[5];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{0, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 0, 0, 0},
						{0, 0, 0, adjustImages.get(3).yRatio, -adjustImages.get(4).yRatio, 0},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 0, 2*Config.THUMBNAIL_PADDING},
						{0, 1, 1, -1, -1,0},
						{1, 1, 1, 0, 0, totalWidth-6*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[5];
				
				calcMatrix(matrix, widths, 5);
				
				for (int i=0; i<5; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(1).outX = 0;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(1).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(0).outX = adjustImages.get(2).outX + adjustImages.get(2).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(3).outX = 0;
				adjustImages.get(3).outY = adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(4).outX = adjustImages.get(3).outX + adjustImages.get(3).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(4).outY = adjustImages.get(3).outY;
				
				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[1] = new ImageRichLinePattern(){
			public int imageCount(){
				return 5;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 5){
					return -1;
				}
				
				for (int i=0; i<5; i++){
					int type = getImageType(images.get(i));
					
					if (IMAGE_LANDSCAPE == type || IMAGE_PANORAMA == type){
						return -1;
					}
				}
				return 5;
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[5];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{0, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 0, 0, 0},
						{0, 0, 0, adjustImages.get(3).yRatio, -adjustImages.get(4).yRatio, 0},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 0, 2*Config.THUMBNAIL_PADDING},
						{0, 1, 1, -1, -1,0},
						{1, 1, 1, 0, 0, totalWidth-6*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[5];
				
				calcMatrix(matrix, widths, 5);
				
				for (int i=0; i<5; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = adjustImages.get(0).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(1).outX + adjustImages.get(1).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(3).outX = adjustImages.get(1).outX;
				adjustImages.get(3).outY = adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(4).outX = adjustImages.get(3).outX + adjustImages.get(3).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(4).outY = adjustImages.get(3).outY;
				
				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[2] = new ImageRichLinePattern(){
			public int imageCount(){
				return 5;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 5){
					return -1;
				}
				
				for (int i=0; i<5; i++){
					int type = getImageType(images.get(i));
					
					if (IMAGE_LANDSCAPE == type || IMAGE_PANORAMA == type){
						return -1;
					}
				}
				return 5;
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[5];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 0, 2*Config.THUMBNAIL_PADDING},
						{adjustImages.get(0).yRatio, 0, -adjustImages.get(2).yRatio, 0, -adjustImages.get(4).yRatio, 2*Config.THUMBNAIL_PADDING},
						{0, 0, 1, 0, -1, 0},
						{0, 1, 0, -1, 0, 0},
						{1, 1, 1, 0, 0, totalWidth-6*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[5];
				
				calcMatrix(matrix, widths, 5);
				
				for (int i=0; i<5; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(1).outX = 0;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(0).outX = adjustImages.get(1).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(0).outX + adjustImages.get(0).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(3).outX = 0;
				adjustImages.get(3).outY = adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(4).outX = adjustImages.get(2).outX;
				adjustImages.get(4).outY = adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
			
		};
	
		patterns[3] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				if (images.size() < 4){
					return -1;
				}

                int aTypes[] = {IMAGE_LANDSCAPE,  IMAGE_SQUARE | IMAGE_PORTRAIT, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {1, 2, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {

				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{0, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 0, 0},
						{0, -1, -1, 1, 2*Config.THUMBNAIL_PADDING},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 0, 0, 1, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(3).outX = adjustImages.get(0).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(3).outY = 0;
				
				adjustImages.get(1).outX = adjustImages.get(3).outX;
				adjustImages.get(1).outY = adjustImages.get(3).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(2).outX = adjustImages.get(1).outX + adjustImages.get(1).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = adjustImages.get(1).outY;
				
				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[4] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}

                int aTypes[] = {IMAGE_LANDSCAPE,  IMAGE_SQUARE | IMAGE_PORTRAIT, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {1, 2, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {

				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{0, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 0, 0},
						{0, -1, -1, 1, 2*Config.THUMBNAIL_PADDING},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 0, 0, 1, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = adjustImages.get(0).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(1).outX+adjustImages.get(1).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(3).outX = adjustImages.get(1).outX;
				adjustImages.get(3).outY = adjustImages.get(1).outHeight+2*Config.THUMBNAIL_PADDING;
				
				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[5] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}

                int aTypes[] = {IMAGE_LANDSCAPE,  IMAGE_SQUARE | IMAGE_PORTRAIT, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {1, 2, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {

				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{0, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 0, 0},
						{0, -1, -1, 1, 2*Config.THUMBNAIL_PADDING},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 0, 0, 1, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(3).outX = 0;
				adjustImages.get(3).outY = 0;
				
				adjustImages.get(0).outX = adjustImages.get(3).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = 0;
				adjustImages.get(1).outY = adjustImages.get(3).outHeight+2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(2).outX = adjustImages.get(1).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = adjustImages.get(1).outY;
				
				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[6] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}

                int aTypes[] = {IMAGE_LANDSCAPE,  IMAGE_SQUARE | IMAGE_PORTRAIT, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {1, 2, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		

				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{0, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 0, 0},
						{0, -1, -1, 1, 2*Config.THUMBNAIL_PADDING},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 0, 0, 1, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(1).outX = 0;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(1).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(0).outX = adjustImages.get(2).outX+adjustImages.get(2).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(3).outX = 0;
				adjustImages.get(3).outY = adjustImages.get(1).outHeight+2*Config.THUMBNAIL_PADDING;

				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[7] = new ImageRichLinePattern(){
			public int imageCount(){
				return 3;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 3){
					return -1;
				}
				
                int aTypes[] = {IMAGE_LANDSCAPE | IMAGE_SQUARE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {2, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 3)){
					return 3;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		

				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[3];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{0, 1, -1, 0},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 1, 0, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[3];
				
				calcMatrix(matrix, widths, 3);
				
				for (int i=0; i<3; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = adjustImages.get(0).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(1).outX;
				adjustImages.get(2).outY = adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;

				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};	
		};
		
		patterns[8] = new ImageRichLinePattern(){
			public int imageCount(){
				return 3;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 3){
					return -1;
				}

                int aTypes[] = {IMAGE_LANDSCAPE | IMAGE_SQUARE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {2, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 3)){
					return 3;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[3];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{0, 1, -1, 0},
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 1, 0, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[3];
				
				calcMatrix(matrix, widths, 3);
				
				for (int i=0; i<3; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}

				adjustImages.get(1).outX = 0;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = 0;
				adjustImages.get(2).outY = adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(0).outX = adjustImages.get(1).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;

				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[9] = new ImageRichLinePattern(){
			public int imageCount(){
				return 5;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 5){
					return -1;
				}
				
				for (int i=0; i<5; i++){
					int type = getImageType(images.get(i));
					if(!(IMAGE_LANDSCAPE == type || IMAGE_SQUARE == type)){
						return -1;
					}
				}
				return 5;
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[5];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{1, -1, 0, 0, 0, 0},
						{0, 0, 1, -1, 0, 0},
						{0, 0, 1, 0, -1, 0},
						{adjustImages.get(0).yRatio, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, -adjustImages.get(4).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 0, 1, 0, 0, totalWidth - 4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[5];
				
				calcMatrix(matrix, widths, 5);
				
				for (int i=0; i<5; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = 0;
				adjustImages.get(1).outY = adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(2).outX = adjustImages.get(0).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(3).outX = adjustImages.get(2).outX;
				adjustImages.get(3).outY = adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(4).outX = adjustImages.get(2).outX;
				adjustImages.get(4).outY = adjustImages.get(3).outY + adjustImages.get(3).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				restoreImageList(images, adjustImages, baseIndex);
				
				return adjustImages.get(0).outHeight + adjustImages.get(1).outHeight + 4*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[10] = new ImageRichLinePattern(){
			public int imageCount(){
				return 5;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 5){
					return -1;
				}
				
				for (int i=0; i<5; i++){
					int type = getImageType(images.get(i));
					if(!(IMAGE_LANDSCAPE == type || IMAGE_SQUARE == type)){
						return -1;
					}
				}
				return 5;
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[5];

				adjustImageList(images, adjustImages, baseIndex);
				
				float matrix[][] = {
						{1, -1, 0, 0, 0, 0},
						{0, 0, 1, -1, 0, 0},
						{0, 0, 1, 0, -1, 0},
						{adjustImages.get(0).yRatio, adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, -adjustImages.get(4).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 0, 1, 0, 0, totalWidth - 4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[5];
				
				calcMatrix(matrix, widths, 5);
				
				for (int i=0; i<5; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}

				adjustImages.get(2).outX = 0;
				adjustImages.get(2).outY = 0;

				adjustImages.get(3).outX = 0;
				adjustImages.get(3).outY = adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;

				adjustImages.get(4).outX = 0;
				adjustImages.get(4).outY = adjustImages.get(3).outY + adjustImages.get(3).outHeight + 2*Config.THUMBNAIL_PADDING;

				adjustImages.get(0).outX = adjustImages.get(2).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;

				adjustImages.get(1).outX = adjustImages.get(0).outX;
				adjustImages.get(1).outY = adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;

				restoreImageList(images, adjustImages, baseIndex);
				
				return adjustImages.get(0).outHeight + adjustImages.get(1).outHeight + 4*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[11] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}

                int aTypes[] = {IMAGE_LANDSCAPE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {2, 2};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		

				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, 0, 0},
						{0, 0, 1, -1, 0},
						{adjustImages.get(0).yRatio, 0, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 1, 1, 0, totalWidth-6*Config.THUMBNAIL_PADDING},						
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = adjustImages.get(0).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(1).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(1).outX + adjustImages.get(1).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(3).outX = adjustImages.get(2).outX;
				adjustImages.get(3).outY = adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;

				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[12] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}
				
                int aTypes[] = {IMAGE_LANDSCAPE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {2, 2};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, 0, 0},
						{0, 0, 1, -1, 0},
						{adjustImages.get(0).yRatio, 0, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 1, 1, 0, totalWidth-6*Config.THUMBNAIL_PADDING},						
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(0).outX = 0;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(2).outX = adjustImages.get(0).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(2).outY = 0;
				
				adjustImages.get(3).outX = adjustImages.get(2).outX;
				adjustImages.get(3).outY = adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(1).outX = adjustImages.get(2).outX + adjustImages.get(2).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(1).outY = 0;

				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[13] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}
				
                int aTypes[] = {IMAGE_LANDSCAPE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {2, 2};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);

				float matrix[][] = {
						{adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, 0, 0, 0},
						{0, 0, 1, -1, 0},
						{adjustImages.get(0).yRatio, 0, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, 2*Config.THUMBNAIL_PADDING},
						{1, 1, 1, 0, totalWidth-6*Config.THUMBNAIL_PADDING},						
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = adjustImages.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				adjustImages.get(2).outX = 0;
				adjustImages.get(2).outY = 0;

				adjustImages.get(3).outX = 0;
				adjustImages.get(3).outY = adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				adjustImages.get(0).outX = adjustImages.get(2).outWidth+2*Config.THUMBNAIL_PADDING;
				adjustImages.get(0).outY = 0;
				
				adjustImages.get(1).outX = adjustImages.get(0).outX + adjustImages.get(0).outWidth + 2*Config.THUMBNAIL_PADDING;
				adjustImages.get(1).outY = 0;

				restoreImageList(images, adjustImages, baseIndex);

				return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
			};
		};
		
		patterns[14] = new ImageRichLinePattern(){
			public int imageCount(){
				return 4;
			}

			public int match(ArrayList<ImageCell> images) {
				
				if (images.size() < 4){
					return -1;
				}
				
				for (int i=0; i<4; i++){
					if (IMAGE_PANORAMA == getImageType(images.get(i))){
						return -1;
					}
				}
				
				if (images.get(0).yRatio / images.get(1).yRatio ==
						images.get(2).yRatio / images.get(3).yRatio){/*Same as single line layout*/
					return -1;
				}
				
				float leftYRatio = images.get(0).yRatio + images.get(2).yRatio;
				float rightYRatio = images.get(1).yRatio + images.get(3).yRatio;
				
				float lrRatio= leftYRatio / rightYRatio;
				float totalYRatio = (leftYRatio*rightYRatio)/(leftYRatio + rightYRatio);
				
				if (lrRatio<0.4f || lrRatio>2.5f || totalYRatio<0.25f || totalYRatio>0.8f){
					return -1;
				}
				
				return 4;
			}

			public int layout(ArrayList<ImageCell> images, int totalWidth) {		
				
				float matrix[][] = {
						{1, 0, -1, 0, 0},
						{0, 1, 0, -1, 0},
						{images.get(0).yRatio, -images.get(1).yRatio, images.get(2).yRatio, -images.get(3).yRatio, 0},
						{1, 1, 0, 0, totalWidth-4*Config.THUMBNAIL_PADDING},
				};
				
				float widths[] = new float[4];
				
				calcMatrix(matrix, widths, 4);
				
				for (int i=0; i<4; i++){
					ImageCell image = images.get(i);
					image.outWidth = (int)widths[i];
					image.outHeight = (int)(image.outWidth * image.yRatio);
				}
				
				images.get(0).outX = 0;
				images.get(0).outY = 0;
				
				images.get(1).outX = images.get(0).outWidth + 2*Config.THUMBNAIL_PADDING;
				images.get(1).outY = 0;
				
				images.get(2).outX = 0;
				images.get(2).outY = images.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				images.get(3).outX = images.get(1).outX;
				images.get(3).outY = images.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
				
				return images.get(0).outHeight + images.get(2).outHeight + 4*Config.THUMBNAIL_PADDING;
			};
			
		};
		
        patterns[15] = new ImageRichLinePattern(){
            public int imageCount(){
                return 4;
            }

            public int match(ArrayList<ImageCell> images) {

                if (images.size() < 4){
                    return -1;
                }
                
                int aTypes[] = {IMAGE_LANDSCAPE | IMAGE_SQUARE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {3, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
            }

            public int layout(ArrayList<ImageCell> images, int totalWidth) {        
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);
            	
                float matrix[][] = {
                        {0, 1, -1, 0, 0},
                        {0, 0, 1, -1, 0},
                        {adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, 4*Config.THUMBNAIL_PADDING},
                        {1, 1, 0, 0, totalWidth - 4*Config.THUMBNAIL_PADDING},
                };
                
                float widths[] = new float[4];
                
                calcMatrix(matrix, widths, 4);
                
                for (int i=0; i<4; i++){
                    ImageCell image = adjustImages.get(i);
                    image.outWidth = (int)widths[i];
                    image.outHeight = (int)(image.outWidth * image.yRatio);
                }
                
                adjustImages.get(0).outX = 0;
                adjustImages.get(0).outY = 0;
                
                adjustImages.get(1).outX = adjustImages.get(0).outWidth + 2*Config.THUMBNAIL_PADDING;
                adjustImages.get(1).outY = 0;
                
                adjustImages.get(2).outX = adjustImages.get(1).outX;
                adjustImages.get(2).outY = adjustImages.get(1).outY + adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
                
                adjustImages.get(3).outX = adjustImages.get(1).outX;
                adjustImages.get(3).outY = adjustImages.get(2).outY + adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;
                
                restoreImageList(images, adjustImages, baseIndex);
                
                return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
            };    
        };
        
        patterns[16] = new ImageRichLinePattern(){
            public int imageCount(){
                return 4;
            }

            public int match(ArrayList<ImageCell> images) {

                if (images.size() < 4){
                    return -1;
                }

                int aTypes[] = {IMAGE_LANDSCAPE | IMAGE_SQUARE, IMAGE_PORTRAIT | IMAGE_SLIM};
                int aNum[] = {3, 1};

                if (true == isImageListPortrait(images, aTypes, aNum, 4)){
					return 4;
				}
				else {
					return -1;
				}
            }

            public int layout(ArrayList<ImageCell> images, int totalWidth) {        
				ArrayList<ImageCell> adjustImages = new ArrayList<ImageCell>();
				int baseIndex[] = new int[4];

				adjustImageList(images, adjustImages, baseIndex);
            	
                float matrix[][] = {
                        {0, 1, -1, 0, 0},
                        {0, 0, 1, -1, 0},
                        {adjustImages.get(0).yRatio, -adjustImages.get(1).yRatio, -adjustImages.get(2).yRatio, -adjustImages.get(3).yRatio, 4*Config.THUMBNAIL_PADDING},
                        {1, 0, 0, 1, totalWidth - 4*Config.THUMBNAIL_PADDING},
                };
                
                float widths[] = new float[4];
                
                calcMatrix(matrix, widths, 4);
                
                for (int i=0; i<4; i++){
                    ImageCell image = adjustImages.get(i);
                    image.outWidth = (int)widths[i];
                    image.outHeight = (int)(image.outWidth * image.yRatio);
                }
                       
                adjustImages.get(1).outX = 0;
                adjustImages.get(1).outX = 0;
                
                adjustImages.get(2).outX = 0;
                adjustImages.get(2).outY = adjustImages.get(1).outY + adjustImages.get(1).outHeight + 2*Config.THUMBNAIL_PADDING;
                
                adjustImages.get(3).outX = 0;
                adjustImages.get(3).outY = adjustImages.get(2).outY + adjustImages.get(2).outHeight + 2*Config.THUMBNAIL_PADDING;
                     
                adjustImages.get(0).outX = adjustImages.get(1).outX + adjustImages.get(1).outWidth + 2*Config.THUMBNAIL_PADDING;
                adjustImages.get(0).outY = 0;

                restoreImageList(images, adjustImages, baseIndex);
                
                return adjustImages.get(0).outHeight + 2*Config.THUMBNAIL_PADDING;
            };    
        };
	}
	
	public int checkPattern(ArrayList<ImageCell> images){
		int minIndex = 0;
		ArrayList<Integer> tempPatterns = new ArrayList<Integer>();
		
		for (int i=0; i<PATTERN_NUM; i++){
			if (patterns[i].match(images) != -1){
				tempPatterns.add(i);
	            if(aPatternMatchSum[i] < aPatternMatchSum[tempPatterns.get(minIndex)]) {
	            	minIndex = tempPatterns.size()-1;
	            }
			}
		}

		availablePatterns.clear();

        for(int i = 0; i < tempPatterns.size(); i++) {
            if(aPatternMatchSum[tempPatterns.get(i)] <= aPatternMatchSum[tempPatterns.get(minIndex)]) {
            	availablePatterns.add(tempPatterns.get(i));
            }
        }

		return availablePatterns.size();
	}
	
	public int getPatternId(int index){
		return availablePatterns.get(index);
	}
	
	public int pickNumForPattern(int index){
		return patterns[availablePatterns.get(index)].imageCount();
	}
	
	public int applyPattern(ArrayList<ImageCell> images, int index, int totalWidth){
		return patterns[availablePatterns.get(index)].layout(images, totalWidth);
	}
	
	public void changePatternMatchSum(int index, int lev){
		aPatternMatchSum[index]+=lev;
	}
	
	ImageRichLinePattern[] patterns;
	ArrayList<Integer> availablePatterns;
	int[] aPatternMatchSum;
}

class ImageRichLineGroup extends ImageLineGroup{
	ImageRichLineGroup(){
	}
	
	void addImages(ArrayList<ImageCell> images){
		imageList = images;
	}
}

public class GalleryLayout {
	private static final int MAX_BUFFER_LENGTH = 5;
	private int lastPattern = -2; /*-1 indicates using the single line, -2 indicates starting*/
	
	GalleryLayout(int containerWidth){
		totalWidth = containerWidth;
		lines = new ArrayList<ImageLineGroup>();
		
		itemBuffer = new ImageProcessBuffer(MAX_BUFFER_LENGTH);
		patterns = new ImageRichLinePatternCollection();
		random = new Random();
	}
	
	public void addLine(ImageLineGroup aLine){
		lines.add(aLine);
	}
	
	private void processImageBuffer(){
		/*Try to find some pattern*/
		int availPattern = patterns.checkPattern(itemBuffer.buffer);
		
		if (availPattern > 0){

			
			int choice;
			
			if (lastPattern == -1){
				choice = random.nextInt(availPattern);
			}
			else {
				choice = random.nextInt(availPattern + 1); /*Introduce the random to cover fall-back single line mode*/
			}
			
			if (choice < availPattern && lastPattern == patterns.getPatternId(choice)) {
				/*Pattern collapse with last one, try to find another*/
				if (availPattern == 1){
					choice = availPattern; /*Using single line*/
				}
				else {
					choice = (choice+1)%(availPattern+1);
				}
			}
			
			if (choice < availPattern){
				ImageRichLineGroup line = new ImageRichLineGroup();
				
				
				int consumeCount = patterns.pickNumForPattern(choice);
				
				ArrayList<ImageCell> images = itemBuffer.shed(consumeCount);
				int height = patterns.applyPattern(images, choice, totalWidth);
				line.addImages(images);
				line.height = height;
				addLine(line);
				lastPattern = patterns.getPatternId(choice);
				patterns.changePatternMatchSum(lastPattern, 1);
			}
			else {
				ImageSingleLineGroup line = new ImageSingleLineGroup(totalWidth);
				while (true == line.needMoreImage() && false == itemBuffer.isEmpty()){
					line.addImage(itemBuffer.remove(0));
				}
				addLine(line);
				lastPattern = -1;
			}
		}
		else {
			/*No pattern found */
			ImageSingleLineGroup line = new ImageSingleLineGroup(totalWidth);
			while (true == line.needMoreImage() && false == itemBuffer.isEmpty()){
				line.addImage(itemBuffer.remove(0));
			}
			addLine(line);
			lastPattern = -1;
		}
	}
	
	public void addImage(long id, int width, int height, int position){
		
		itemBuffer.add(new ImageCell(id, width, height, position));
		
		if (itemBuffer.isFull()){
			processImageBuffer();
		}
	}
	
	public void addImageFinish(){
		while (!itemBuffer.isEmpty()){
			processImageBuffer();
		}
	}
	
	public int getLineNum(){
		return lines.size();
	}
	
	public ArrayList<ImageLineGroup> lines = null;
	private  ImageProcessBuffer itemBuffer = null;
	private ImageRichLinePatternCollection patterns = null;
	private Random random = null;
	private int totalWidth = 0;
}

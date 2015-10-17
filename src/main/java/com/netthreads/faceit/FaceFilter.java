package com.netthreads.faceit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

@SuppressWarnings("unused")
public class FaceFilter
{
	// Load OpenCV library.
	static
	{
		nu.pattern.OpenCV.loadLibrary();

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	private static Logger logger = LoggerFactory.getLogger(FaceFilter.class);

	private static final String CATALOG_FOLDER = "catalog";

	private static String CATALOG = "/artwork_data.csv";
	private static String CLASSIFIER_HAARCASCADE = "/haarcascade_frontalface_alt.xml";
	private static String CLASSIFIER_FRONTALFACE = "/lbpcascade_frontalface.xml";

	private static final int URL_INDEX = 18;
	private static final int ARTIST_INDEX = 2;
	private static final int TITLE_INDEX = 5;

	private CascadeClassifier faceDetector;
	private MatOfRect faceDetections;

	/**
	 * Construct face filter.
	 * 
	 */
	public FaceFilter()
	{
		// Create a face detector from the cascade file in the resources
		// directory.
		faceDetector = new CascadeClassifier(getClass().getResource(CLASSIFIER_HAARCASCADE).getPath());

		// Detect faces in the image.
		// MatOfRect is a special container class for Rect.
		faceDetections = new MatOfRect();
	}

	/**
	 * Build filtered catalog of images with faces only.
	 * 
	 * @param sourceFile
	 * @param catalogPath
	 * @throws IOException
	 */
	private void buildFilteredCatalog(String sourceFile, String catalogPath) throws IOException
	{

		InputStream is = getClass().getResourceAsStream(sourceFile);

		if (is != null)
		{
			Reader reader = new InputStreamReader(is);

			CSVReader csvReader = new CSVReader(reader);

			String[] nextLine;
			// Read past the title line.
			if (csvReader.readNext() != null)
			{
				// If there are entries them make target folder.
				String targetFolder = getWorkingDirectory() + "/" + CATALOG_FOLDER;
				makeFolder(targetFolder);

				while ((nextLine = csvReader.readNext()) != null)
				{
					String url = nextLine[URL_INDEX];
					if (url != null && !url.isEmpty())
					{
						String artist = nextLine[ARTIST_INDEX].replace(',', '_').replace(' ', '_');
						String title = nextLine[TITLE_INDEX].replace(',', '_').replace(' ', '_');
						
						String tag = artist + "_" + title;

						extractFacesToFile(targetFolder, tag, url);
					}
				}
			}

			csvReader.close();
		}
	}

	/**
	 * Extract any faces and save them against tag.
	 * 
	 * @param tag
	 * @param url
	 */
	private void extractFacesToFile(String folder, String tag, String url)
	{
		String filePath = saveImageToTemp(url);

		if (filePath != null)
		{
			Mat image = Highgui.imread(filePath);

			// NOTE : We seem to be able re-use this okay.
			faceDetector.detectMultiScale(image, faceDetections);

			Rect[] detections = faceDetections.toArray();
			
			int faceCount = detections.length;

			if (faceCount > 0)
			{
				logger.info(String.format("Detected %s faces in %s", faceCount, tag));

				// Draw a bounding box around each face
				int index = 0;
				for (Rect rect : detections)
				{
					Rect rectCrop = new Rect(rect.x, rect.y, rect.width, rect.height);

					Mat image_roi = new Mat(image, rectCrop);

					// Save the visualized detection.
					String filename = folder + "/" + tag + "_" + index + ".png";

					logger.info(String.format("Writing %s", filename));

					Highgui.imwrite(filename, image_roi);

					index++;
				}
			}
		}
	}

	/**
	 * Download image to temporary file.
	 * 
	 * @param imageUrl
	 * 
	 * @return path to temporary image.
	 */
	private String saveImageToTemp(String imageUrl)
	{
		String path = null;
		try
		{
			URL url = new URL(imageUrl);

			path = getWorkingDirectory() + "/temp.jpg";

			InputStream inputStream = url.openStream();
			OutputStream outputStream = new FileOutputStream(path);

			byte[] b = new byte[2048];
			int length;

			while ((length = inputStream.read(b)) != -1)
			{
				outputStream.write(b, 0, length);
			}

			inputStream.close();
			outputStream.close();
		}
		catch (Exception e)
		{
			logger.error(e.getLocalizedMessage());
		}

		return path;
	}

	/**
	 * Return working directory.
	 * 
	 * @return The working directory.
	 */
	private String getWorkingDirectory()
	{
		String working = System.getProperty("user.dir");

		return working;
	}

	/**
	 * Make folder.
	 * 
	 * @param folderName
	 */
	private void makeFolder(String folderName)
	{
		File file = new File(folderName);

		if (!file.exists())
		{
			file.mkdir();
		}
	}

	/*
	 * 1. Parse CSV files for thumb-nails 2. For each image download it and
	 * check for faces. 3. If faces found then extract and tag them.
	 */
	public static void main(String[] args)
	{
		FaceFilter faceFilter = new FaceFilter();

		try
		{
			faceFilter.buildFilteredCatalog(CATALOG, CATALOG_FOLDER);
		}
		catch (IOException e)
		{
			// Error
			logger.error(e.getLocalizedMessage());
		}

	}

}

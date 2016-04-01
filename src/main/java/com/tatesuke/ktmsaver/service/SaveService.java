package com.tatesuke.ktmsaver.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tatesuke.ktmsaver.dto.Request;
import com.tatesuke.ktmsaver.dto.Response;
import com.tatesuke.ktmsaver.ui.KTMFileDialog;

/**
 * ファイル保存サービス
 * 
 * @author tatesuke
 */
public class SaveService {

	private static final String BACKUPFILE_SYNBOL = "ktmbk";
	private KTMFileDialog dialog = getKTMFileDialog();
	private boolean isClosed = false;

	protected KTMFileDialog getKTMFileDialog() {
		return new KTMFileDialog();
	}

	FileOutputStream fileOutputStream;
	File file;

	/**
	 * 新規保存。
	 * <p>
	 * ファイルダイアログを表示し、ユーザが指定したファイルにrequest.contentを書き込む。
	 * </p>
	 * <p>
	 * request.fileDirで指定したディレクトリがダイアログの初期ディレクトリとなる。
	 * request.fileDirがnullであっても構わない。その場合初期ディレクトリがどこになるかは分からない。
	 * </p>
	 * <p>
	 * request.fileNameで指定したディレクトリがダイアログの初期ファイル名となる。
	 * request.fileNameはnullであってはならない。
	 * </p>
	 * 
	 * @param request
	 *            リクエスト
	 * @return 以下を格納したresponseオブジェクトを返す。
	 * 
	 *         保存成功時
	 *         <ul>
	 *         <li>response.result = Response.Result.SUCCESS
	 *         <li>response.filePath = 保存したファイルの絶対パス
	 *         <li>response.message = null
	 *         </ul>
	 * 
	 *         キャンセル時
	 *         <ul>
	 *         <li>response.result = Response.Result.CANCEL
	 *         <li>response.filePath = null
	 *         <li>response.message = null
	 *         </ul>
	 * 
	 *         保存失敗時
	 * 
	 *         <ul>
	 *         <li>response.result = Response.Result.ERROR
	 *         <li>response.filePath = null
	 *         <li>response.message = エラーメッセージ
	 *         </ul>
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 * @throws IOException
	 * 
	 */
	public void startSaveAs(Request request) throws InvocationTargetException,
			InterruptedException, IOException {
		if (request == null) {
			throw new IllegalArgumentException("request is null");
		}
		if (isClosed) {
			throw new IllegalStateException("already closed.");
		}
		if (fileOutputStream != null) {
			throw new IllegalStateException("already started");
		}

		// ファイルダイアログの表示
		File baseDir = (request.fileDir == null) ? null : new File(
				request.fileDir);
		String fileName = request.fileName;
		file = dialog.getFile(baseDir, fileName);

		// ファイルに応じた処理
		if (file != null) {
			if (request.backupEnabled == true) {
				backup(file, request.backupDir, request.backupGeneration);
			}
			fileOutputStream = new FileOutputStream(file);
		}
	}

	/**
	 * 上書き保存。
	 * <p>
	 * request.fileDir, request.fileNameで指定したファイルにrequest.contentを書き込む。
	 * </p>
	 * <p>
	 * request.fileDirおよびrequest.fileNameはnullであってはならない。
	 * </p>
	 * 
	 * @param request
	 *            リクエスト
	 * @return 以下を格納したresponseオブジェクトを返す。
	 * 
	 *         保存成功時
	 *         <ul>
	 *         <li>response.result = Response.Result.SUCCESS
	 *         <li>response.filePath = 保存したファイルの絶対パス
	 *         <li>response.message = null
	 *         </ul>
	 * 
	 *         キャンセル時
	 *         <ul>
	 *         <li>response.result = Response.Result.CANCEL
	 *         <li>response.filePath = null
	 *         <li>response.message = null
	 *         </ul>
	 * 
	 *         保存失敗時
	 * 
	 *         <ul>
	 *         <li>response.result = Response.Result.ERROR
	 *         <li>response.filePath = null
	 *         <li>response.message = エラーメッセージ
	 *         </ul>
	 * @throws FileNotFoundException
	 * 
	 */
	public void startOverwriteSave(Request request)
			throws FileNotFoundException {
		if (request == null) {
			throw new IllegalArgumentException("request is null");
		}
		if (isClosed) {
			throw new IllegalStateException("already closed.");
		}

		file = new File(request.fileDir, request.fileName);

		if (request.backupEnabled == true) {
			backup(file, request.backupDir, request.backupGeneration);
		}

		fileOutputStream = new FileOutputStream(file);
	}

	private void backup(File file, String backupDirPath, int backupGeneration) {
		if (!file.isFile()) {
			return;
		}

		File backupDir = new File(backupDirPath);
		if (!backupDir.isAbsolute()) {
			backupDir = new File(file.getParentFile(), backupDirPath);
		}

		if (!backupDir.isDirectory()) {
			boolean mkdirs = backupDir.mkdirs();
			if (mkdirs == false) {
				throw new RuntimeException("バックアップディレクトリを作成できませんでした。");
			}
		}

		List<File> backUpFileList = Stream
				.of(backupDir.listFiles())
				.filter(f -> f.isFile())
				.filter(f -> f.getName().startsWith(file.getName()))
				.filter(f -> {
					String afterName = f.getName().substring(
							file.getName().length());
					return afterName.matches("\\." + BACKUPFILE_SYNBOL
							+ "[1-9][0-9]*");
				}).sorted((f1, f2) -> {
					int n1 = getFileNumber(file, f1);
					int n2 = getFileNumber(file, f2);
					return n1 - n2;
				}).collect(Collectors.toList());

		if ((0 < backupGeneration)
				&& (backupGeneration < (backUpFileList.size() + 1))) {
			int deleteNum = backUpFileList.size() + 1 - backupGeneration;
			for (int i = 0; i < deleteNum; i++) {
				backUpFileList.get(i).delete();
			}
		}

		int maxNumber = 0;
		if (!backUpFileList.isEmpty()) {
			File leatestFile = backUpFileList.get(backUpFileList.size() - 1);
			maxNumber = getFileNumber(file, leatestFile);
		}
		int newNumber = maxNumber + 1;
		String newName = file.getName() + "." + BACKUPFILE_SYNBOL + newNumber;
		File newFile = new File(backupDir, newName);
		file.renameTo(newFile);
	}

	private int getFileNumber(File baseFile, File file) {
		int nameLength = baseFile.getName().length();
		String afterName = file.getName().substring(nameLength);
		String numberStr = afterName.substring(BACKUPFILE_SYNBOL.length() + 1);
		return Integer.parseInt(numberStr);
	}

	public void append(byte[] data) throws IOException {
		if (data == null) {
			throw new IllegalArgumentException("data is null");
		}
		if (isClosed) {
			throw new IllegalStateException("already closed.");
		}

		try {
			fileOutputStream.write(data);
		} catch (IOException e) {
			fileOutputStream.close();
			fileOutputStream = null;
			throw (IOException) e.fillInStackTrace();
		}
	}

	public void close(Response response) throws IOException {
		if (fileOutputStream == null) {
			throw new IllegalStateException("fileOutputStream is null");
		}

		fileOutputStream.close();
		isClosed = true;

		if (response == null) {
			return;
		}

		if (file != null) {
			response.result = Response.Result.SUCCESS;
			response.filePath = file.getCanonicalPath();
			response.message = null;
		} else {
			response.result = Response.Result.CANCEL;
			response.filePath = null;
			response.message = null;
		}
	}

	public boolean isClosed() {
		return isClosed;
	}

}

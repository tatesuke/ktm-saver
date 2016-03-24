package com.tatesuke.ktmsaver.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

	protected KTMFileDialog getKTMFileDialog() {
		return new KTMFileDialog();
	}

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
	 * 
	 */
	public Response saveAs(Request request) {
		synchronized (this) {
			Response response = new Response();

			File file = null;
			try {
				File baseDir = (request.fileDir == null) ? null : new File(
						request.fileDir);
				String fileName = request.fileName;
				file = dialog.getFile(baseDir, fileName);
			} catch (Exception e1) {
				e1.printStackTrace();
				response.result = Response.Result.ERROR;
				response.filePath = null;
				response.message = e1.getMessage();
				return response;
			}

			if (file == null) {
				response.result = Response.Result.CANCEL;
				response.filePath = null;
				response.message = null;
			} else {
				save(request, response, file);
			}

			return response;
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
	 * 
	 */
	public Response overwrite(Request request) {
		synchronized (this) {
			Response response = new Response();

			File file = new File(request.fileDir, request.fileName);
			save(request, response, file);

			return response;
		}
	}

	private void save(Request request, Response response, File file) {
		if (request.backupEnabled == true) {
			File baseDir = new File(request.backupDir);
			if (!baseDir.isAbsolute()) {
				baseDir = new File(file.getParentFile(), request.backupDir);
			}
			backup(file, baseDir, request.backupGeneration);
		}
		try (FileOutputStream fo = new FileOutputStream(file)) {
			byte[] bytes = request.content.getBytes("utf-8");
			fo.write(bytes);

			response.result = Response.Result.SUCCESS;
			response.filePath = file.getAbsolutePath();
			response.message = null;
		} catch (IOException e) {
			e.printStackTrace();
			response.result = Response.Result.ERROR;
			response.filePath = null;
			response.message = e.getMessage();
		}
	}

	private void backup(File file, File backupDir, int backupGeneration) {
		if (!file.isFile()) {
			return;
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
					return afterName.matches("\\." + BACKUPFILE_SYNBOL + "[1-9][0-9]*");
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
}

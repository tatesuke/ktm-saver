package com.tatesuke.ktmsaver.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import com.tatesuke.ktmsaver.dto.Request;
import com.tatesuke.ktmsaver.dto.Response;
import com.tatesuke.ktmsaver.ui.KTMFileDialog;

/**
 * ファイル保存サービス
 * 
 * @author tatesuke
 */
public class SaveService {

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
			try (FileWriter fw = new FileWriter(file)) {
				fw.append(request.content);
				response.result = Response.Result.SUCCESS;
				response.filePath = file.getAbsolutePath();
				response.message = null;
			} catch (IOException e) {
				response.result = Response.Result.ERROR;
				response.filePath = null;
				response.message = e.getMessage();
			}

			return response;
		}
	}

}

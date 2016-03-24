package ktmsaver;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import com.tatesuke.ktmsaver.ui.KTMFileDialog;

public class KTMFileDialogTest {

	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
		KTMFileDialog dialog = new KTMFileDialog();
		File file = dialog.getFile(null, "pom.xml");
		System.out.println(file.getAbsolutePath());
	}
	
}

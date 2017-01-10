import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class shareBox extends Thread {

	private WatchService watcher = null;
	private Map<WatchKey, Path> keys = null;
	// private final boolean recursive = false;
	private boolean trace = false;

	shareBox(Path dir1, boolean recursive) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();

		register(dir1);

		// enable trace after initial registration
		this.trace = true;
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private void register(Path dir1) throws IOException {
		WatchKey key = dir1.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir1);
			} else {
				if (!dir1.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir1);
				}
			}
		}
		keys.put(key, dir1);
	}

	void processEvents() throws IOException, InterruptedException {
		for (;;) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir1 = keys.get(key);
			if (dir1 == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();

				// TBD - provide example of how OVERFLOW event is handled
				if (kind == OVERFLOW) {
					continue;
				}

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir1.resolve(name);
				System.out.print("Nome: " + name);
				// print out event
				System.out.format(" %s: %s\n", event.kind().name(), child);
				if (kind == ENTRY_CREATE) {
					// System.out.println("criei o arquivo: " + child);
					// String sDir = name.toString();
					// String extensionRemovedj = sDir.split("\\.")[0];
					menu(1, name.toString());
				} else {
					// System.out.println("deletei o arquivo: " + child);
					// String sDir = name.toString();
					// String extensionRemovedj = sDir.split("\\.")[0];
					menu(2, name.toString());
				}
				// if directory is created, and watching recursively, then
				// register it and its sub-directories
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}

	}

	static void usage() {
		System.err.println("usage: java WatchDir [-r] dir");
		System.exit(-1);
	}

	shareBox() {
		bucketName = "shareboxsample";
		keyName = "dif";
		uploadFileName = "/home/yanick/Documentos/SD/dif.txt";
		dir = "/home/yanick/Documentos/SD/dir/";
	}

	static void menu(int op, String docName) throws IOException {
		AWSCredentials Credentials = new BasicAWSCredentials(
				"AKIAI2AGRDLZAS5QOJQQ",
				"GmM0E/JwqInGaa9ty/OSgMMZW5fmgRmp9j9Gtg1Z");
		// System.out
		// .println("Entre com uma OP:\n0: Sair\n1: Upload\n2: Delete\n3: DownLoad\n4: Listar Objetos\n5: Listar objetos do dir\n6 Dif entre nuvem e Dir\n7 Funcionar");
		// Scanner in = new Scanner(System.in);
		// int op = in.nextInt();
		shareBox s3client = new shareBox();
		switch (op) {

		case 1:
			s3client.uploadfileFuncione(Credentials, docName);
			break;

		case 2:
			s3client.deletefileFuncione(Credentials, docName);
			break;

		case 3:
			s3client.downloadfile(Credentials);
			break;

		case 4:
			// s3client.listingFilesNuvem(Credentials);
			break;

		case 5:
			// listaDir();

			break;

		case 6:
			difNuvemDir(Credentials, null);
			break;
		case 7:
			funcione(Credentials);
			break;
		default:
			System.out.println("###Default case, Error###");
		}
	}

	// Pra cima é tudo novo

	private static String bucketName = "shareboxsample";
	private static String keyName = "dif";// TEM QUE MUDAR AQUI TBM PARA FAZER O
											// Uacho PLOAD
	private static String uploadFileName = "/home/yanick/Documentos/SD/dif.txt";
	private static String dir = "/home/yanick/Documentos/SD/dir/";
	private static String fileNameGlobal;

	// void uploadfile(AWSCredentials credentials) {
	// AmazonS3 s3client = new AmazonS3Client(credentials);
	//
	// try {
	// System.out.println("Uploading a new object to S3 from a file\n");
	// File file = new File(uploadFileName);
	// s3client.putObject(new PutObjectRequest(bucketName, keyName, file));
	// System.out.println("File uloaded\n");
	//
	// } catch (AmazonServiceException ase) {
	// System.out.println("Caught an AmazonServiceException, which "
	// + "means your request made it "
	// + "to Amazon S3, but was rejected with an error response"
	// + " for some reason.");
	// System.out.println("Error Message:    " + ase.getMessage());
	// System.out.println("HTTP Status Code: " + ase.getStatusCode());
	// System.out.println("AWS Error Code:   " + ase.getErrorCode());
	// System.out.println("Error Type:       " + ase.getErrorType());
	// System.out.println("Request ID:       " + ase.getRequestId());
	// } catch (AmazonClientException ace) {
	// System.out.println("Caught an AmazonClientException, which "
	// + "means the client encountered "
	// + "an internal error while trying to "
	// + "communicate with S3, "
	// + "such as not being able to access the network.");
	// System.out.println("Error Message: " + ace.getMessage());
	// }
	// }

	// void deletefile(AWSCredentials credentials1) {
	// AmazonS3 s3client = new AmazonS3Client(credentials1);
	// try {
	// s3client.deleteObject(new DeleteObjectRequest(bucketName, keyName));
	// } catch (AmazonServiceException ase) {
	// System.out.println("Caught an AmazonServiceException.");
	// System.out.println("Error Message:    " + ase.getMessage());
	// System.out.println("HTTP Status Code: " + ase.getStatusCode());
	// System.out.println("AWS Error Code:   " + ase.getErrorCode());
	// System.out.println("Error Type:       " + ase.getErrorType());
	// System.out.println("Request ID:       " + ase.getRequestId());
	// } catch (AmazonClientException ace) {
	// System.out.println("Caught an AmazonClientException.");
	// System.out.println("Error Message: " + ace.getMessage());
	// }
	// }

	void downloadfile(AWSCredentials credentials2) throws IOException {
		AmazonS3 s3client = new AmazonS3Client(credentials2);
		StringBuilder textoQueSeraEscrito = new StringBuilder();

		try {
			System.out.println("Downloading an object");
			S3Object s3object = s3client.getObject(new GetObjectRequest(
					bucketName, keyName));
			System.out.println("Content-Type: "
					+ s3object.getObjectMetadata().getContentType());
			InputStream input = s3object.getObjectContent();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					input));
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				textoQueSeraEscrito.append(line);
				System.out.println("    " + line);
			}
			String fileName = keyName;
			FileWriter arquivo;
			try {
				arquivo = new FileWriter(new File("" + dir + keyName + ".txt"));

				arquivo.write(textoQueSeraEscrito.toString());
				arquivo.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println();
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which"
					+ " means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means"
					+ " the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	public static void difNuvemDir(AWSCredentials credentials5, String qualquer)
			throws IOException {
		// POPULA O ARRAY DA NUVEM
		ArrayList<String> dirFiles = new ArrayList<String>();
		ArrayList<String> nuvemFiles = new ArrayList<String>();
		AmazonS3 s3client = new AmazonS3Client(credentials5);
		AmazonS3 s3 = new AmazonS3Client(credentials5);

		ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
				.withBucketName(bucketName));

		for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
			if (!nuvemFiles.contains(objectSummary.getKey()))// houve um upload
				nuvemFiles.add(objectSummary.getKey());
		}
		// POPULA O ARRAY DO DIRETORIO
		File folder = new File("/home/yanick/Documentos/SD/dir");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {

			if (listOfFiles[i].isFile()) {
				if (!dirFiles.contains(listOfFiles[i].getName()))
					dirFiles.add(listOfFiles[i].getName());
			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
			}
		}

		int[] vetPresenteNuvem;
		vetPresenteNuvem = new int[dirFiles.size()];
		int[] vetPresenteDir;
		vetPresenteDir = new int[nuvemFiles.size()];

		// INICIA VETORES COM 0
		for (int i = 0; i < dirFiles.size(); i++) {
			vetPresenteNuvem[i] = 0;
		}
		for (int i = 0; i < nuvemFiles.size(); i++) {
			vetPresenteDir[i] = 0;
		}

		// PROCURA COMPARANDO CADA UM DO DIRETORIO COM TODOS DA NUVEM
		for (int i = 0; i < dirFiles.size(); i++) {
			String s = dirFiles.get(i);
			String extensionRemoved = s.split("\\.")[0];
			for (int j = 0; j < nuvemFiles.size(); j++) {
				String sNuvem = nuvemFiles.get(j);
				String extensionRemovedj = sNuvem.split("\\.")[0];
				if (extensionRemovedj.compareTo(extensionRemoved) == 0) {
					vetPresenteNuvem[i] = 1;// presente
				} else {
					// System.out.println("Ausente: " + extensionRemovedj);
				}
			}
		}

		for (int i = 0; i < nuvemFiles.size(); i++) {
			String s = nuvemFiles.get(i);
			String extensionRemoved = s.split("\\.")[0];
			for (int j = 0; j < dirFiles.size(); j++) {
				String sDir = dirFiles.get(j);
				String extensionRemovedj = sDir.split("\\.")[0];
				if (extensionRemovedj.compareTo(extensionRemoved) == 0) {
					vetPresenteDir[i] = 1;// presente
				} else {
					// System.out.println("Ausente: " + extensionRemovedj);
				}
			}
		}

		// PROCURA COMPARANDO CADA UM DA NUVEM COM TODOS DO DIRETORIO

		System.out
				.println("||||||||||||||||||||||||||||||||||||||||||||||||||");

		for (int i = 0; i < dirFiles.size(); i++) {
			if (vetPresenteNuvem[i] == 1) {

				System.out.println("Presente Arquivo do DIRETÓRIO: "
						+ dirFiles.get(i));
			} else {

				System.out.print("Ausente na nuvem - " + dirFiles.get(i));
				// deletar no dir
				System.out.println(" Ação: deletar: " + dir + dirFiles.get(i));
				String nome = dir + dirFiles.get(i);
				File f = new File(nome);
				f.delete();
				for (int x = 0; x < nuvemFiles.size(); x++) {
					if (nuvemFiles.get(x).compareTo(dirFiles.get(i)) == 0) {
						nuvemFiles.remove(x);
						break;
					}
				}
				dirFiles.remove(i);
			}
		}
		System.out.println();
		for (int i = 0; i < nuvemFiles.size(); i++) {

			if (vetPresenteDir[i] == 1) {
				System.out.println("Presente Arquivo da NUVEM: "
						+ nuvemFiles.get(i));
			} else {
				System.out.print("Ausente no dir " + nuvemFiles.get(i));
				System.out.println(" Ação -> Baixar Arquivo "
						+ nuvemFiles.get(i) + " da nuvem");
				downloadfileFuncione(credentials5, nuvemFiles.get(i));
			}
		}

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out
				.println("\n###############################################\n");

	}

	// boolean contains(Object element): Retorna verdadeiro se a lista contém o
	// elemento especificado e falso caso contrário.
	// Object remove(int index): Remove o i-ésimo elemento da lista.
	// Usado para entender como funciona verificar as coisas na nuvem
	// void listingFilesNuvem(AWSCredentials credentials4) {
	//
	// AmazonS3 s3 = new AmazonS3Client(credentials4);
	//
	// System.out.println("Listing objects");
	// ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
	// .withBucketName(bucketName));
	// for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries())
	// {
	// // System.out.println(" - " + objectSummary.getKey() + "  "
	// // + "(size = " + objectSummary.getSize() + ")");
	// nuvemFiles.add(objectSummary.getKey());
	//
	// }
	// for (int i = 0; i < nuvemFiles.size(); i++) {
	// String s = nuvemFiles.get(i);
	// System.out.println(s);
	// }
	// System.out.println();
	// }
	//
	// static void listaDir() {
	// File folder = new File("/home/yanick/Documentos/SD/dir");
	// File[] listOfFiles = folder.listFiles();
	//
	// for (int i = 0; i < listOfFiles.length; i++) {
	// if (listOfFiles[i].isFile()) {
	// // System.out.println("File /home/yanick/Documentos/SD/dir/"
	// // + listOfFiles[i].getName());
	// dirFiles.add(listOfFiles[i].getName());
	// } else if (listOfFiles[i].isDirectory()) {
	// System.out.println("Directory " + listOfFiles[i].getName());
	// }
	// }
	// for (int i = 0; i < dirFiles.size(); i++) {
	// String s = dirFiles.get(i);
	// String extensionRemoved = s.split("\\.")[0];
	// System.out.println(extensionRemoved);
	// }
	// }

	static void funcione(AWSCredentials credentials) {
		System.out.println("Criando um arquivo no diretório " + dir);
		criaFile(credentials);
	}

	static void registerDirectory() throws IOException, InterruptedException {
		// try {
		// System.out
		// .println("\n########################8000#######################\n");
		// Thread.sleep(4000);
		// } catch (InterruptedException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		// try {
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		Path dir1 = Paths.get(dir);
		new shareBox(dir1, false).processEvents();

	}

	static void criaFile(AWSCredentials credentials) {
		String fileName = null;
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
		// System.out.println(sdf.format(cal.getTime()));
		String textoQueSeraEscrito = "Texto que sera escrito."
				+ sdf.format(cal.getTime());
		FileWriter arquivo;
		try {
			arquivo = new FileWriter(new File("" + dir + "Arquivo"
					+ sdf.format(cal.getTime()) + ".txt"));
			fileName = "" + dir + "Arquivo" + sdf.format(cal.getTime())
					+ ".txt";
			arquivo.write(textoQueSeraEscrito);
			arquivo.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void deletefileFuncione(AWSCredentials credentials1, String fileName) {
		AmazonS3 s3client = new AmazonS3Client(credentials1);
		try {
			s3client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	static void uploadfileFuncione(AWSCredentials credentials, String fileName) {

		AmazonS3 s3client = new AmazonS3Client(credentials);

		try {
			System.out.println("Uploading a new object to S3 from a file\n");
			File file = new File(dir + fileName);// diretório do arquivo
			s3client.putObject(new PutObjectRequest(bucketName, fileName, file));
			System.out.println("File uloaded\n");

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which "
					+ "means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which "
					+ "means the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	static void downloadfileFuncione(AWSCredentials credentials2,
			String fileName) throws IOException {
		AmazonS3 s3client = new AmazonS3Client(credentials2);
		try {

			System.out.println("Downloading an object");
			s3client.getObject(new GetObjectRequest(bucketName, fileName),
					new File("/home/yanick/Documentos/SD/dir/" + fileName));

		} catch (AmazonServiceException ase) {
			System.out.println("Caught an AmazonServiceException, which"
					+ " means your request made it "
					+ "to Amazon S3, but was rejected with an error response"
					+ " for some reason.");
			System.out.println("Error Message:    " + ase.getMessage());
			System.out.println("HTTP Status Code: " + ase.getStatusCode());
			System.out.println("AWS Error Code:   " + ase.getErrorCode());
			System.out.println("Error Type:       " + ase.getErrorType());
			System.out.println("Request ID:       " + ase.getRequestId());
		} catch (AmazonClientException ace) {
			System.out.println("Caught an AmazonClientException, which means"
					+ " the client encountered "
					+ "an internal error while trying to "
					+ "communicate with S3, "
					+ "such as not being able to access the network.");
			System.out.println("Error Message: " + ace.getMessage());
		}
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {
		Runnable r1 = new Runnable() {
			public void run() {
				while (true) {
					try {
						menu(6, null);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		Thread t1 = new Thread(r1);
		t1.start();

		Runnable r = new Runnable() {
			public void run() {
				while (true) {
					try {
						registerDirectory();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.start();
		// registerDirectory();
	}

}


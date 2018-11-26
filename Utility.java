import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by vladimirkallin on 23/11/2018.
 */

public class Utility {

    static int numberOfFlows = 0;
    static String pathToFile = "";
    static String nameOfDir = "";

    public static String ERROR_MESSAGE = "Использование java -jar Utility\n" +
            "  Количество одновременно качающих потоков (1,2,3,4....)\n" +
            "  Путь к файлу со списком ссылок\n" +
            "  Имя папки, куда складывать скачанные файлы\n";


    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.print(ERROR_MESSAGE);
            return;
        } else {
            try {
                numberOfFlows = Integer.valueOf(args[0].replaceAll("[^0-9]", ""));
                pathToFile = String.valueOf(args[1].replaceAll("-", ""));
                nameOfDir = String.valueOf(args[2].replaceAll("-", ""));
            } catch(Exception exc) {
                exc.printStackTrace();
                return;
            }
        }


        System.out.println("Параметры: \n");
        System.out.println("Количество потоков: " +  numberOfFlows);
        System.out.println("Путь к файлу: " + pathToFile);
        System.out.println("Имя папки: " + nameOfDir);

        TaskFile taskFile = new TaskFile(pathToFile);

        System.out.printf("Получены задания: \n");
        for (Task task : taskFile.getTasks()) {
            System.out.printf("Путь:" + task.getPath() + " в файл: " + task.getFileName() + "\n");
        }
        System.out.printf("\n");

        File file = new File(nameOfDir);
        if (!file.exists() || !file.isDirectory()) {
            System.out.printf("Неверное имя папки для сохранения: " + nameOfDir);
            return;
        }


        for (Task task : taskFile.getTasks()) {
            Task itask = task;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Downloader dl = new Downloader();
                    try {
                        dl.getFile(itask.getPath());
                    } catch (IOException exc) {
                        exc.printStackTrace();
                        return;
                    }
                    String pathToWriteFile = nameOfDir+"/"+itask.getFileName();
                    File fileToWrite = new File(pathToWriteFile);
                    try {
                        FileWriter writer = new FileWriter(fileToWrite);
                        writer.write(dl.getBufferOfChars());
                        writer.close();
                    } catch (IOException e) {
                        System.out.printf("%s", e.toString());
                        return;
                    }
                    System.out.println("Содержимое из: "+ itask.getPath() + " скачано и записано в файл: " + pathToWriteFile + "\n");
                }


            }).start();
        }
    }
}

interface DownloaderInterface {
    byte[] getFile(String pathToURL) throws IOException;
}

class Downloader implements DownloaderInterface {

    private byte[] buffer;

    @Override
    public byte[] getFile(String pathToURL) throws IOException {
        URL connection = new URL(pathToURL);
        HttpURLConnection urlConnection;
        urlConnection = (HttpURLConnection) connection.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        InputStream in = urlConnection.getInputStream();

        int buffSize = in.available();
        buffer = new byte[buffSize];
        in.read(buffer);

        in.close();

        return buffer;
    }

    public Downloader() {
    }

    public byte[] getBuffer() {
        return buffer;
    }

    public void setBuffer(byte[] buffer) {
        this.buffer = buffer;
    }

    public char[] getBufferOfChars() {
        char[] chars = new char[this.buffer.length];
        for (int i=0;i<this.buffer.length;i++) {
            chars[i] = (char) this.buffer[i];
        }
        return chars;
    }
}

class Task {
    private String path;
    private String fileName;

    public Task(String path, String fileName) {
        this.path = path;
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

class TaskFile {
    private String fileName;
    private ArrayList<Task> tasks;

    public TaskFile(String fileName) {
        this.fileName = fileName;
        this.tasks = new ArrayList<>();

        File taskFile = new File(fileName);
        if (taskFile.exists() && taskFile.isFile()) {
            try {
                FileReader reader = new FileReader(taskFile);
                char[] buffer = new char[(int)taskFile.length()];
                reader.read(buffer);
                parse(buffer);
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void parse(char[] buffer) {
        String fileAsString = new String(buffer);
        String[] words = fileAsString.trim().replaceAll("\r", " ").split(" ");
        for (int i=0;i<words.length-1;i+=2) {
            Task task = new Task(words[i], words[i+1]);
            tasks.add(task);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void setTasks(ArrayList<Task> tasks) {
        this.tasks = tasks;
    }

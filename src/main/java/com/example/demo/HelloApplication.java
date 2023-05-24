package com.example.demo;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class HelloApplication extends Application {
    private final int MAX_CACHE_SIZE = 10;
    private final LinkedList<String> queue = new LinkedList<>();
    private javafx.scene.input.Clipboard clipboard;
    private Stage primaryStage;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        this.primaryStage = primaryStage;

        Group root = new Group();
        Scene scene = new Scene(root, Color.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle("pasteman");
        primaryStage.show();

        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            TrayIcon trayIcon = new TrayIcon(ImageIO.read(new File("/Users/hrtps/Pictures/IMG_1252.JPG")));
            trayIcon.setToolTip("pasteman");
            SystemTray tray = SystemTray.getSystemTray();
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                throw new RuntimeException(e);
            }
        }

        clipboard = javafx.scene.input.Clipboard.getSystemClipboard();

        // 全局事件监听，监听系统快捷键Ctrl+Shift+V
        registerShortcut();

        // 监控剪贴板
        scanClipboard();
    }

    /**
     * 不断扫描剪贴板，识别新复制的内容
     */
    private void scanClipboard() {
        Executors.newSingleThreadExecutor().submit(() -> {
            Clipboard clipboard2 = Toolkit.getDefaultToolkit().getSystemClipboard();
            String previousContent = "";
            while (true) {
                try {
                    // 检查当前剪切板中是否有文本
                    if (!clipboard2.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                        continue;
                    }
                    // 获取剪切板中的文本内容
                    String content = (String) clipboard2.getData(DataFlavor.stringFlavor);

                    // 如果和上一次获取到的内容不同，则表示有新数据加入
                    if (!content.equals(previousContent)) {
                        System.out.println("新条目：" + content);
                        queue.addFirst(content);
                        // 最多保留10条，如果超过，则删除最旧的一条
                        if (queue.size() > MAX_CACHE_SIZE) {
                            queue.removeLast();
                        }
                        System.out.println("当前list：" + queue);
                        previousContent = content;
                    }
                    // 每1s扫码一次
                    Thread.sleep(100);
                } catch (UnsupportedFlavorException | InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void registerShortcut() {
        Provider provider = Provider.getCurrentProvider(false);
//        provider.register(KeyStroke.getKeyStroke("ctrl shift V"), new HotKeyListener() {
//            @Override
//            public void onHotKey(HotKey hotKey) {
//                System.out.println(hotKey.toString());
//            }
//        });

        provider.unregister(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() + InputEvent.SHIFT_DOWN_MASK));
        provider.register(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() + InputEvent.SHIFT_DOWN_MASK), new HotKeyListener() {
            @Override
            public void onHotKey(HotKey hotKey) {
                System.out.println(hotKey.toString());
                showPopup();
            }
        });
    }

    /**
     * 显示缓存列表
     */
    private void showPopup() {
        ListView<String> listView = new ListView<>();
        listView.setCursor(Cursor.NONE);
        listView.setMouseTransparent(true);
        listView.setPrefSize(200, 200);
        listView.setItems(FXCollections.observableList(queue));

        listView.setOnMouseClicked(event -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            // 处理列表项被点击后的逻辑，比如打印选中的项
            System.out.println("选择了：" + selected);
            if (selected != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(selected);
                clipboard.setContent(content);
                firePaste();
            }
        });

        Platform.runLater(() -> {
            Scene scene = new Scene(listView, 320, queue.size() * 30 + 20);
            Stage stage = new Stage();
            stage.initOwner(primaryStage);
            stage.setScene(scene);
            stage.setAlwaysOnTop(true);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            // 监听弹出窗口所在场景的聚焦状态
//                stage.getScene().getWindow().focusedProperty().addListener((observable, oldValue, newValue) -> {
//                    // 如果失去聚焦，则隐藏弹出窗口
//                    if (!newValue) {
//                        stage.hide();
//                    }
//                });

//                Point location = MouseInfo.getPointerInfo().getLocation();
//                stage.setX(location.x);
//                stage.setY(location.y);
            stage.showAndWait();
        });
    }

    /**
     * 在当前鼠标位置插入指定长度的文本
     */
    private void firePaste() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
}


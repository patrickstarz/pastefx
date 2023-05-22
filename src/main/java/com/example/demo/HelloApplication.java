package com.example.demo;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.Provider;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Executors;

public class HelloApplication extends Application {
    private final int MAX_CACHE_SIZE = 10;
    private final LinkedList<String> queue = new LinkedList<>();
    private javafx.scene.input.Clipboard clipboard;


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
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
                    Thread.sleep(1000);
                } catch (UnsupportedFlavorException | InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private void registerShortcut() {
//        new Thread(() -> {
//            try {
//                GlobalScreen.registerNativeHook();
//            } catch (NativeHookException e) {
//                throw new RuntimeException(e);
//            }
//            GlobalScreen.addNativeKeyListener(new AppGlobalKeyListener());
//
//        }).start();
        new Thread(() -> {
            Provider provider = Provider.getCurrentProvider(false);
//            provider.register(KeyStroke.getKeyStroke("ctrl shift V"), new HotKeyListener() {
//                @Override
//                public void onHotKey(HotKey hotKey) {
//                    System.out.println(hotKey.toString());
//                }
//            });
            provider.register(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() + java.awt.event.InputEvent.SHIFT_MASK), new HotKeyListener() {
                @Override
                public void onHotKey(HotKey hotKey) {
                    showPopup();
                }
            });
        }).start();
    }

    /**
     * 显示缓存列表
     */
    private void showPopup() {
        Pane popupPane = new Pane();
        for (int i = 0; i < queue.size(); i++) {
            String cache = queue.get(i);
            Text text = new Text(cache);
            text.setWrappingWidth(300);
            text.setFont(Font.font(14));
            text.setLayoutX(10);
            text.setLayoutY((i + 1) * 30);

            // 添加点击事件
            text.setOnMouseClicked(event -> {
//                if (cache instanceof String) {
                clipboard.setContent(new ClipboardContent());
//                } else if (cache instanceof javafx.scene.image.Image) {
//                    clipboard.setContent(new ClipboardContent());
//                }
//                Point2D mousePoint = root.sceneToLocal(event.getScreenX(), event.getScreenY());
//                moveCursor(mousePoint, 50);
                firePaste();
            });
            popupPane.getChildren().add(text);
        }
        Platform.runLater(() -> {
            Scene popupScene = new Scene(popupPane, 320, queue.size() * 30 + 20);
            Stage popupStage = new Stage();
            popupStage.setScene(popupScene);

            Point location = MouseInfo.getPointerInfo().getLocation();
            popupStage.setX(location.x);
            popupStage.setY(location.y);
            popupStage.showAndWait();
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
            throw new RuntimeException(e);
        }
    }
}


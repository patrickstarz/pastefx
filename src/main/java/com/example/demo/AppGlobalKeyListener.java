package com.example.demo;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

public class AppGlobalKeyListener implements NativeKeyListener {
    private LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(3);

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {

    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeEvent) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));

        try {
            queue.put(e.getKeyCode());
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        // Ctrl + Shift + V
        int[] hotKeyArray = {NativeKeyEvent.VC_CONTROL, NativeKeyEvent.VC_SHIFT, NativeKeyEvent.VC_V};
        if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException nativeHookException) {
                nativeHookException.printStackTrace();
            }
        }

        //如果队列中的数据大于等于3，那就进行判断是不是包含连续且等于我们指定的键的顺序
        if (queue.size() >= 3 && judgeCombinationKey(hotKeyArray)) {
            System.out.println("按下了Ctrl+Shift+V");
            queue.clear();
        }
        if (queue.size() == 4) {
            queue.poll();
        }
    }

    private Boolean judgeCombinationKey(int[] hotKeyArray) {
        Object[] queueKey = queue.toArray();

        Predicate<int[]> keyArrayPredicateOne = hotKeys -> (int) queueKey[0] == hotKeys[0]
                && (int) queueKey[1] == hotKeys[1]
                && (int) queueKey[2] == hotKeys[2];

        Predicate<int[]> keyArrayPredicateTwo = hotKeys -> (int) queueKey[1] == hotKeys[0]
                && (int) queueKey[2] == hotKeys[1]
                && (int) queueKey[3] == hotKeys[2];

        return queue.size() == 3 ? keyArrayPredicateOne.test(hotKeyArray) : keyArrayPredicateOne.or(keyArrayPredicateTwo).test(hotKeyArray);
    }
}

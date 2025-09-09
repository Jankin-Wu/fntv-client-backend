package com.jankinwu.fntv.client.config;

import com.jankinwu.fntv.client.enums.TranscodingModeEnum;

public class TranscodingModeConfig {

    // volatile关键字确保多线程环境下的可见性和有序性
    private static volatile TranscodingModeConfig instance;

    // 硬件加速启用标志
    private boolean enableHardwareAcceleration = true;

    private TranscodingModeEnum transcodingMode;

    // 私有构造函数，防止外部实例化
    private TranscodingModeConfig() {
        // 防止通过反射创建实例
        if (instance != null) {
            throw new RuntimeException("请使用getInstance()方法获取实例");
        }
    }

    // 公共静态方法获取单例实例
    public static TranscodingModeConfig getInstance() {
        // 第一次检查，避免不必要的同步
        if (instance == null) {
            // 同步代码块，确保线程安全
            synchronized (TranscodingModeConfig.class) {
                // 第二次检查，确保只创建一个实例
                if (instance == null) {
                    instance = new TranscodingModeConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 获取硬件加速标志
     * @return 硬件加速标志
     */
    public boolean isEnableHwAccel() {
        if (transcodingMode == TranscodingModeEnum.HW_ONLY) {
            enableHardwareAcceleration = true;
        } else if (transcodingMode == TranscodingModeEnum.HW_SW_SWITCH) {
            enableHardwareAcceleration = !enableHardwareAcceleration;
        } else if (transcodingMode == TranscodingModeEnum.SW_ONLY) {
            enableHardwareAcceleration = false;
        }
        return enableHardwareAcceleration;
    }

    /**
     * 设置转码模式
     * @param transcodingMode 转码模式
     */
    public void setTranscodingMode(String transcodingMode) {
        this.transcodingMode = TranscodingModeEnum.getByCode(transcodingMode);
    }
}

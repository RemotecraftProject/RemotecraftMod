package com.zireck.remotecraft;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by Zireck on 07/08/16.
 */
public class QrCodeManager {

    private final String mIpAddress;
    private ResourceLocation mResourceLocation = null;

    public QrCodeManager(String ipAddress) {
        mIpAddress = ipAddress;
    }

    public void generateQrCode() {
        if (mResourceLocation != null) {
            return;
        }

        BufferedImage bufferedImage = null;
        File imageFile = QRCode.from(mIpAddress).to(ImageType.PNG).withSize(256, 256).file();

        try {
            bufferedImage = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mResourceLocation = Minecraft.getMinecraft().renderEngine.getDynamicTextureLocation(imageFile.getName(), new DynamicTexture(bufferedImage));
    }

    public void renderQrCode(GuiScreen guiScreen) {
        if (mResourceLocation != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(mResourceLocation);
            guiScreen.drawModalRectWithCustomSizedTexture(guiScreen.width / 2 - 64, guiScreen.height / 2 - (90), 0, 0, 128, 128, 128, 128);
        }
    }
}

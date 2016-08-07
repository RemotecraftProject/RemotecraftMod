package com.zireck.remotecraft;

import net.glxn.qrgen.core.image.ImageType;
import net.glxn.qrgen.javase.QRCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by Zireck on 06/08/16.
 */
public class RemotecraftGui extends GuiScreen {

    private ResourceLocation mResourceLocation = null;
    private GuiButton mButtonClose;

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.add(mButtonClose = new GuiButton(0, this.width / 2 - 100, this.height - (this.height / 4) + 10, "Close GUI"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == mButtonClose) {
            mc.thePlayer.closeScreen();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        generateQrCode();
        renderQrCode();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void generateQrCode() {
        if (mResourceLocation != null) {
            return;
        }

        BufferedImage bufferedImage = null;
        File imageFile = QRCode.from("hello!").to(ImageType.PNG).withSize(256, 256).file();

        try {
            bufferedImage = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mResourceLocation = Minecraft.getMinecraft().renderEngine.getDynamicTextureLocation(imageFile.getName(), new DynamicTexture(bufferedImage));
    }

    private void renderQrCode() {
        if (mResourceLocation != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(mResourceLocation);
            drawModalRectWithCustomSizedTexture(width / 2 - 64, height / 2 - (90), 0, 0, 128, 128, 128, 128);
        }
    }
}
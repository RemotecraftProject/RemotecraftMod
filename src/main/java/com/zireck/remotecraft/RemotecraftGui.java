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

    //private GuiButton a;

    @Override
    public void initGui() {
        //this.buttonList.add(this.a = new GuiButton(0, this.width / 2 - 100, this.height / 2 - 24, "This is button a"));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        /*if (button == this.a) {
            System.out.println("Button clicked!!!");
        }*/
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        renderQrCode();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    private void renderQrCode() {
        ResourceLocation resourceLocation = null;
        BufferedImage bufferedImage = null;
        File imageFile = QRCode.from("hello!").to(ImageType.JPG).file();

        try {
            bufferedImage = ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        resourceLocation = Minecraft.getMinecraft().renderEngine.getDynamicTextureLocation(imageFile.getName(), new DynamicTexture(bufferedImage));
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);

        this.drawTexturedModalRect(this.width / 2 - 128, this.height / 2 - 128, 0, 0, 256, 256);
    }
}
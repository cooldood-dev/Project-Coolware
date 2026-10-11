package coolware.font.novoline.impl;

import coolware.font.novoline.api.FontFamily;
import coolware.font.novoline.api.FontManager;
import coolware.font.novoline.api.FontType;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * @author Artyom Popov
 * @since June 30, 2020
 */
public final class SimpleFontManager implements FontManager {

	//region instantiating
	private SimpleFontManager() {}

	public static FontManager create() {
		return new SimpleFontManager();
	}
	//endregion

	private static final String FONT_DIRECTORY = "novoline/fonts/";
	private final FontRegistry fonts = new FontRegistry();

	@Override
	public FontFamily fontFamily(FontType fontType) {
		return fonts.fontFamily(fontType);
	}

	private static final class FontRegistry extends EnumMap<FontType, FontFamily> {

		private FontRegistry() { super(FontType.class); }

		private FontFamily fontFamily(FontType fontType) {
			return computeIfAbsent(fontType, ignored -> {
				try {
					return SimpleFontFamily.create(fontType, readFontFromResources(fontType));
				} catch(IOException e) {
					throw new RuntimeException("Failed to read font: " + fontType, e);
				}
			});
		}

		//region reading font
		private static java.awt.Font readFontFromResources(FontType fontType) throws IOException {
			InputStream stream = null;
			try {
				IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
				ResourceLocation location = new ResourceLocation("novoline/fonts/" + fontType.fileName());
				IResource resource = resourceManager.getResource(location);
				if (resource != null) {
					stream = resource.getInputStream();
				}
			} catch (Throwable ignored) {
			}

			if (stream == null) {
				stream = SimpleFontManager.class.getResourceAsStream("/assets/minecraft/novoline/fonts/" + fontType.fileName());
			}
			if (stream == null) {
				stream = SimpleFontManager.class.getClassLoader().getResourceAsStream("assets/minecraft/novoline/fonts/" + fontType.fileName());
			}
			if (stream == null) {
				throw new IOException("Couldn't find font resource for " + fontType.fileName());
			}

			try (InputStream resourceInputStream = stream) {
				return readFont(resourceInputStream);
			}
		}

		private static java.awt.Font readFont(InputStream resource) {
			java.awt.Font font;

			try {
				font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, resource);
			} catch(FontFormatException e) {
				throw new RuntimeException("Resource does not contain the required font tables for the specified format", e);
			} catch(IOException e) {
				throw new RuntimeException("Couldn't completely read font resource", e);
			}

			return font;
		}
		//endregion
	}
}

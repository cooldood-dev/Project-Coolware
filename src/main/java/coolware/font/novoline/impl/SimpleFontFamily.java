package coolware.font.novoline.impl;

import coolware.font.novoline.api.FontFamily;
import coolware.font.novoline.api.FontRenderer;
import coolware.font.novoline.api.FontType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Artyom Popov
 * @since July 04, 2020
 */
final class SimpleFontFamily implements FontFamily {

	private final FontType fontType;
	private final java.awt.Font awtFont;
	private final Map<Integer, FontRenderer> cache = new ConcurrentHashMap<Integer, FontRenderer>();

	private SimpleFontFamily(FontType fontType, java.awt.Font awtFont) {
		this.fontType = fontType;
		this.awtFont = awtFont;
	}

	static FontFamily create(FontType fontType, java.awt.Font awtFont) {
		return new SimpleFontFamily(fontType, awtFont);
	}

	@Override
	public FontRenderer ofSize(int size) {
		return cache.computeIfAbsent(size, ignored -> {
			return SimpleFontRenderer.create(awtFont.deriveFont(java.awt.Font.PLAIN, size));
		});
	}

	@Override
	public FontType font() { return fontType; }
}
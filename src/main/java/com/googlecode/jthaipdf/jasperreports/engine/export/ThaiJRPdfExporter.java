/*
 * The MIT License
 *
 * Copyright (c) 2008 Virasak Dungsrikaew (virasak@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jthaipdf.jasperreports.engine.export;

import java.text.AttributedCharacterIterator;
import java.util.Locale;
import java.util.Map;

import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.pdf.common.PdfTextChunk;

import com.googlecode.jthaipdf.util.ThaiDisplayUtils;


/**
 * A {@link JRPdfExporter} (JasperReports 7.x, {@code net.sf.jasperreports.pdf})
 * that rearranges Thai glyphs so combining vowels and tone marks do not overlap
 * the base consonant in the exported PDF.
 *
 * <p>Unlike the JasperReports 4.x version, this no longer wraps an iText
 * {@code Chunk}: the modern exporter passes the chunk text in as a {@code String}
 * and builds the (producer-agnostic) {@link PdfTextChunk} itself, so we only need
 * to transform the text with {@link ThaiDisplayUtils#toDisplayString(String)}
 * before delegating to the superclass.
 *
 * <p>Note: this approach maps characters onto Private Use Area glyphs and
 * therefore requires an embedded font that carries the legacy Thai PUA glyph set.
 * For modern TrueType fonts, prefer JasperReports' built-in glyph renderer by
 * setting the report/context property
 * {@code net.sf.jasperreports.export.pdf.glyph.renderer.blocks.x=thai}.
 *
 * @author Virasak Dungsrikaew (virasak@gmail.com)
 * @author Wasan Anusornhirunkarn (wasan@sah.co.th) - JasperReports 7.x upgrade
 */
public class ThaiJRPdfExporter extends JRPdfExporter {

	@Override
	protected PdfTextChunk getChunk(Map<AttributedCharacterIterator.Attribute, Object> attributes,
			String text, Locale locale) {
		return super.getChunk(attributes, ThaiDisplayUtils.toDisplayString(text), locale);
	}
}

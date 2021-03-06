package com.instantolap.charts.impl.legend;

import com.instantolap.charts.Cube;
import com.instantolap.charts.Data;
import com.instantolap.charts.impl.data.Theme;
import com.instantolap.charts.impl.util.SymbolDrawer;
import com.instantolap.charts.renderer.ChartColor;
import com.instantolap.charts.renderer.ChartFont;
import com.instantolap.charts.renderer.Renderer;
import com.instantolap.charts.renderer.popup.Popup;


public class LegendImpl extends BasicLegendImpl {

  private static final int SYMBOL_SPACE = 4;

  private Theme theme;
  private Data data;
  private Cube cube;

  public LegendImpl(Theme theme) {
    this.theme = theme;
  }

  @Override
  public void setData(Data data) {
    this.data = data;
    this.cube = data.getCurrentCube();
  }

  @Override
  public void render(
    double progress,
    final Renderer r,
    double x, double y,
    double totalWidth, double totalHeight,
    ChartColor foreground, ChartColor background,
    ChartFont font) {
    // calc size
    final double padding = getPadding();
    final double spacing = getSpacing();
    final boolean isVertical = isVertical();
    final ChartColor[] colors = getColors();

    final double max = isVertical ? totalHeight : totalWidth;
    final double[] size = getNeededSize(r, max, font);
    final double width = size[0];
    final double height = size[1];

    // center
    // x += (totalWidth - width) / 2;
    // y += (totalHeight - height) / 2;

    // set font
    ChartFont usedFont = getFont();
    if (usedFont == null) {
      usedFont = font;
    }
    r.setFont(usedFont);

    // draw border
    final double roundedCorner = getRoundedCorner();

    final ChartColor legendBackground = getBackground();
    if (legendBackground != null) {

      // draw shadow
      final ChartColor shadow = getShadow();
      if (shadow != null) {
        final double xOffset = getShadowXOffset();
        final double yOffset = getShadowYOffset();
        r.setColor(shadow);
        r.fillRoundedRect(x + xOffset, y + yOffset, width, height, roundedCorner);
      }

      r.setColor(legendBackground);
      r.fillRoundedRect(x, y, width, height, roundedCorner);
    }

    final ChartColor border = getBorder();
    if (border != null) {
      r.setColor(border);
      r.setStroke(getBorderStroke());
      r.drawRoundedRect(x, y, width, height, roundedCorner);
      r.resetStroke();
    }

    // draw content
    x += padding;
    y += padding;
    double xx = 0, yy = 0;
    double maxTextWidth = 0, maxTextHeight = 0;

    final String[] content = getContent();
    for (int n = 0; n < content.length; n++) {
      final String text = content[n];

      // calc text size
      double textWidth = r.getTextWidth(text);
      final double textHeight = r.getTextHeight(text);
      textWidth += SYMBOL_SPACE + textHeight;

      // new row?
      if (isVertical) {
        if (yy + textHeight > max) {
          yy = 0;
          x += maxTextWidth + spacing;
        }
      } else {
        if (xx + textWidth > max) {
          xx = 0;
          y += maxTextHeight + spacing;
        }
      }

      // draw symbol
      final int symbol = data.getSymbol(n);

      ChartColor seriesColor;
      if ((colors != null) && (n < colors.length)) {
        seriesColor = colors[n];
      } else {
        seriesColor = data.getColor(theme, 0, n);
      }

      final int dimension = getDimension();
      final boolean isVisible = cube.isVisible(dimension, n);
      if (!isVisible) {
        seriesColor = seriesColor.setOpacity(0.33);
      }

      final double popupX = x + xx;
      final double popupY = y + yy;

      SymbolDrawer.draw(r, symbol, x + xx + textHeight / 2, y + yy + textHeight / 2, textHeight,
        seriesColor, seriesColor, background
      );

      // draw text
      ChartColor color = getColor();
      if (color == null) {
        color = foreground;
      }

      if (!isVisible) {
        color = color.setOpacity(0.33);
      }

      r.setColor(color);
      r.drawText(x + xx + textHeight + SYMBOL_SPACE, y + yy + textHeight / 2, text, 0,
        Renderer.WEST, false
      );

      final int sample = n;

      final Runnable highlightCommand = () -> {
        try {
          if (isVisible) {
            final Integer selected = data.getSelectedSample(dimension);
            if (selected == null || selected != sample) {
              data.setSelectedSample(dimension, sample);
              r.showClickPointer();
              r.fireRepaint(false);
            }
          }
        } catch (Exception e) {
          r.showError(e);
        }
      };

      final Runnable unHighlightCommand = () -> {
        try {
          final Integer selected = data.getSelectedSample(dimension);
          if (selected != null) {
            data.setSelectedSample(dimension, null);
            r.showNormalPointer();
            r.fireRepaint(false);
          }
        } catch (Exception e) {
          r.showError(e);
        }
      };

      final Runnable visibleCommand = () -> {
        try {
          cube.setVisible(dimension, sample, !cube.isVisible(dimension, sample));
          r.fireRepaint(false);
        } catch (Exception e) {
          r.showError(e);
        }
      };

      final Popup popup = r.addPopup(
        popupX, popupY,
        textHeight + SYMBOL_SPACE + textWidth, textHeight, 0,
        Renderer.EAST, null, null,
        highlightCommand, unHighlightCommand, visibleCommand
      );

      final Integer selectedSample = data.getSelectedSample(dimension);
      if (selectedSample != null && selectedSample == sample) {
        r.setCurrentPopup(popup);
      }

      // next position
      if (isVertical) {
        yy += textHeight + spacing;
      } else {
        xx += textWidth + spacing;
      }

      maxTextWidth = Math.max(textWidth, maxTextWidth);
      maxTextHeight = Math.max(textHeight, maxTextHeight);
    }
  }

  @Override
  public double[] getNeededSize(Renderer r, double maxSize, ChartFont font) {
    final double padding = getPadding();
    final double spacing = getSpacing();

    ChartFont usedFont = getFont();
    if (usedFont == null) {
      usedFont = font;
    }
    r.setFont(usedFont);

    final boolean isVertical = isVertical();
    double maxWidth = 0, maxHeight = 0;
    double currentWidth = 0, currentHeight = 0;
    double maxTextWidth = 0, maxTextHeight = 0;

    boolean isFirst = true;
    final String[] content = getContent();
    for (final String text : content) {
      // calc text size
      double textWidth = r.getTextWidth(text);
      final double textHeight = r.getTextHeight(text);
      textWidth += SYMBOL_SPACE + textHeight; // symbol space
      maxTextWidth = Math.max(maxTextWidth, textWidth);
      maxTextHeight = Math.max(maxTextHeight, textHeight);

      if (isVertical) {
        // new column?
        final double neededHeight = textHeight + (isFirst ? 0 : spacing);
        isFirst = false;
        if (currentHeight + neededHeight > maxSize) {
          currentWidth += maxTextWidth + spacing;
          maxTextWidth = textWidth;
          isFirst = true;
          currentHeight = 0;
        }

        // add space
        currentHeight += neededHeight;
      } else {
        // new row?
        final double neededWidth = textWidth + (isFirst ? 0 : spacing);
        isFirst = false;
        if (currentWidth + neededWidth > maxSize) {
          currentHeight += maxTextHeight + spacing;
          maxTextHeight = textHeight;
          isFirst = true;
          currentWidth = 0;
        }

        // add space
        currentWidth += neededWidth;
      }

      maxWidth = Math.max(maxWidth, currentWidth);
      maxHeight = Math.max(maxHeight, currentHeight);
    }

    if (isVertical) {
      currentWidth += maxTextWidth;
    } else {
      currentHeight += maxTextHeight;
    }

    maxWidth = Math.max(maxWidth, currentWidth);
    maxHeight = Math.max(maxHeight, currentHeight);

    double totalWidth = 2 * padding;
    double totalHeight = 2 * padding;
    if (isVertical) {
      totalWidth += currentWidth;
      totalHeight += maxHeight;
    } else {
      totalWidth += maxWidth;
      totalHeight += currentHeight;
    }
    return new double[]{totalWidth, totalHeight};
  }

  private String[] getContent() {
    final String[] labels = getLabels();

    final int count = cube.getSampleCount(getDimension());

    final String[] result = new String[count];
    for (int n = 0; n < result.length; n++) {
      final int i = isReverse() ? (result.length - 1 - n) : n;
      if ((labels != null) && (i < labels.length)) {
        result[i] = labels[n];
      } else {
        result[i] = cube.getSample(getDimension(), n);
      }
    }
    return result;
  }
}

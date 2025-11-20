package com.dakkra.hypersynesthesia.ffmpeg;

import com.dakkra.hypersynesthesia.BarStyle;
import com.dakkra.hypersynesthesia.OutputFormat;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.file.Path;

@Data
@Accessors( fluent = true )
public class RenderSettings {

	private String prefix;

	private Path sourcePath;

	private int width;

	private int height;

	private int frameRate;

	private Color backgroundColor;

	private Path backgroundImage;

	private BarStyle barStyle;

	private Color barColor;

	private Path targetPath;

	private OutputFormat outputFormat;

}

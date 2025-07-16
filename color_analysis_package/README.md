# Color Analysis Script

This script allows you to analyze colors and regions of interest (ROIs) in images using OpenCV.

## Requirements
- Python 3.x
- OpenCV (`opencv-python`)
- imutils

Install the required dependencies using:
```bash
pip install -r requirements.txt
```

## How to Use

```bash
python colores.py <image_file> <comparison_type>
```

### Arguments:
- `<image_file>`: Path to the image file you want to analyze.
- `<comparison_type>`: Type of comparison:
  - `"1"`: Mean value of cyan channel in ROI.
  - `"2"`: Cyan mean divided by (mean of blue + mean of green).
  - `"3"`: Advanced analysis based on red/blue ratios.
  - `"4"`: Full analysis including grayscale and advanced metrics.

## Example
```bash
python colores.py example.jpg 4
```

This will run the script on `example.jpg` using comparison type 4.

## Notes
- The script allows manual selection of ROIs.
- It displays the computed results in the console.

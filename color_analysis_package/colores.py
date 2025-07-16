#!/usr/bin/env python3
# Script to analyze colors in images using OpenCV

import cv2  # Library for image processing
import imutils  # Helper library for image manipulation
import argparse  # Standard library for command-line argument parsing

# Argument parser configuration
parser = argparse.ArgumentParser(description='Mean value of ROI in an image')
parser.add_argument("image", type=str, help="Image file")
parser.add_argument("tipo", type=str, help="Type of comparison")
args = parser.parse_args()

print(args)

# Read the image and resize it
orig = cv2.imread(args.image)
height = orig.shape[0]
orig2 = imutils.resize(orig, height=int(height / 3.0))

# Split color channels
cyan = 255 - orig[:, :, 2]
red = orig[:, :, 2]
blue = orig[:, :, 0]
green = orig[:, :, 1]

# Select Regions of Interest (ROIs)
rois = cv2.selectROIs('orig', orig2, fromCenter=False, showCrosshair=False)
rois = rois * 3  # Adjust ROI size after resizing

means = []

# Processing according to the comparison type
if args.tipo == "4":
    gray = cv2.cvtColor(orig, cv2.COLOR_BGR2GRAY)
    for r in rois:
        means.append(cyan[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean())
        means.append(cyan[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean() /
                     (blue[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean() +
                      green[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean()))
        blue_count = 0
        weighted = 0
        for x in range(r[0], r[0] + r[2]):
            for y in range(r[1], r[1] + r[3]):
                gray_value = float(gray[y, x])
                if (gray_value / 255 > 0.30):
                    if (float(red[y, x]) / 255 < 0.8):
                        ratio = float(red[y, x]) / blue[y, x]
                        if (ratio < 0.8):
                            blue_count += 1
                            weighted += (50 * (0.81 - ratio))
        means.append(blue_count)
        means.append(weighted)

    print("Mean Value: ")
    for i in range(len(means) // 4):
        print(str(means[i * 4]))
    print("Mean Value / (Mean Blue + Mean Green): ")
    for i in range(len(means) // 4):
        print(str(means[i * 4 + 1]))
    print("Red/Blue Ratio: ")
    for i in range(len(means) // 4):
        print(str(means[i * 4 + 2]))
    print("Weighted Red/Blue Ratio: ")
    for i in range(len(means) // 4):
        print(str(means[i * 4 + 3]))
else:
    gray = cv2.cvtColor(orig, cv2.COLOR_BGR2GRAY)
    for r in rois:
        if args.tipo == "1":
            means.append(cyan[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean())
        elif args.tipo == "2":
            means.append(cyan[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean() /
                         (blue[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean() +
                          green[r[1]:r[1] + r[3], r[0]:r[0] + r[2]].mean()))
        elif args.tipo == "3":
            weighted = 0
            blue_count = 0
            for x in range(r[0], r[0] + r[2]):
                for y in range(r[1], r[1] + r[3]):
                    gray_value = float(gray[y, x])
                    if (gray_value / 255 > 0.30):
                        if (float(red[y, x]) / 255 < 0.8):
                            ratio = float(red[y, x]) / blue[y, x]
                            if (ratio < 0.8):
                                blue_count += 1
                                weighted += (50 * (0.81 - ratio))
            means.append(blue_count)
            means.append(weighted)
    for i in range(len(means)):
        print(str(i + 1) + " value = " + str(means[i]))

cv2.destroyAllWindows()

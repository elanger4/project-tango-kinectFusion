import fileinput
for line in fileinput.FileInput("PointCloudActivityBinary", inplace=1):
    line = line.replace("1","(y)")
    line = line.replace("0","(n)")
    print line,


#########################################################################################################
# @Author: --
# @Description: Use Regex to truncate float numbers to fixed precision (p=4)
#########################################################################################################
import re


PATTERN = '\d+\.\d{4,}'


def match_and_trunc(line):
    searched = re.findall(PATTERN, line)
    if len(searched) > 0:
        replaced = ["{:.4f}".format(float(x)) for x in searched]
    counter = 0
    for s in searched:
        line = line.replace(s, replaced[counter])
        counter+=1
    return line


def analyze(input_file, output_file):
    lines_list = []
    with open(input_file, 'r') as fp:
        for line in fp:
            lines_list.append(match_and_trunc(line))
            if len(lines_list) > 5000:
                with open(output_file, 'a+') as fw:
                    fw.writelines(lines_list)
                lines_list = []
                print('5000 analysed...')
        with open(output_file, 'a+') as fw:
            fw.writelines(lines_list)


if __name__ == "__main__":
    read_dir = '/mnt/yago2/ehsan/dev/yago2geo/all/'
    write_dir = '/mnt/yago2/ehsan/dev/yago2geo/trunc/'
    files = ['cleaned_OSNI_extended.ttl', 'cleaned_OS_extended.ttl', 'cleaned_OSI_new.ttl',
             'cleaned_OSI_new.ttl', 'cleaned_OSNI_new.ttl', 'cleaned_OSI_extended.ttl',
             'cleaned_OSM_extended.ttl', 'cleaned_GADM_extended.ttl', 'cleaned_GADM_new.ttl']
    for file in files:
        print('************{}***********'.format(file))
        analyze(read_dir+file, write_dir+file)


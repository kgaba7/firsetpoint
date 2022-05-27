package controller;

import com.opencsv.CSVReader;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import log.Logger;
import model.EngineParamFir;
import model.FirModel;
import model.Index;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import utils.Constants;
import utils.Credentials;

import java.io.*;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FirSetpointController {
    private Logger logger;
    private List<String> downloaded;
    private List<FirModel> firModelList;

    public FirSetpointController(Logger logger) {
        this.logger = logger;
        this.downloaded = new LinkedList<>();
        this.firModelList = new LinkedList<>();
    }

    public void processFile(String path, int resolution, boolean isCompressed) throws IOException {
        Reader reader;

        if (isCompressed) {
            reader = readCompressedFile(path);
        } else {
            reader = new FileReader(path);
        }


        LocalDate fileDate = getLocalDateFromPath(path);
        FirModel actModel;

        if (reader != null && fileDate != null) {
            CSVReader csvReader = new CSVReader(reader);
            LocalDate actDate = LocalDate.of(1999, 1, 1);
            LocalTime midnight = LocalTime.MIDNIGHT;
            LocalTime actTime;
            double value;

            int idx = 0;
            String[] line;
            Index index = new Index();

            while ((line = csvReader.readNext()) != null) {
                String[] record;

                if (idx == 0) {
                    record = line[0].split(";");
                    removeQM(record);


                    setIndexes(record, index);


                    idx++;
                    continue;
                }

                record = line[0].split(";");
                removeQM(record);

                if (record.length > 2) {
                    try {
                        actDate = string2LocalDateTime(record[index.getActtim()]).toLocalDate();

                        //TODO ez nem biztos, hogy kell, lehet FIR fájloknál irreleváns
                        //log fájlok elejére néha bekerülnek előző napi adatok. ezeket átugorjuk
                        if (actDate.compareTo(fileDate) != 0) {
                            idx++;
                            continue;
                        }

                        actTime = string2LocalDateTime(record[index.getActtim()]).toLocalTime();


                        if (isRecordNeeded(record, index)) {
                            try {
                                value = Double.parseDouble(record[index.getMeaval()]);
                                if (value <= 0) {
                                    continue;
                                }
                            } catch (NumberFormatException ex) {
//                                getLogger().error(TFRProcessorV3.class.getName(), "Format error in file \"" + getFileNameFromPath(path) + "\" at row " + rowCount + " column " + index.getMeaval());
                                continue;
                            } catch (Exception ex) {
//                                getLogger().error(TFRProcessorV3.class.getName(), "Error in file \"" + path + "\" at line " + idx + 1 + ". Message: " + ex.getMessage());
                                continue;
                            }


                            actModel = getFirModelByName(firModelList, record, index);

                            if (actModel == null) {
                                FirModel firModel = new FirModel(record[index.getArnam()], record[index.getMotnam()], record[index.getName()]);
                                firModel.setActValue(Double.valueOf(record[index.getMeaval()]));
                                this.firModelList.add(firModel);
                                actModel = firModel;
                            } else {
                                //TODO ha új utasítás ugyanaz, mint előző, akkor figyelmen kívül hagyjuk
                                actModel.setPrevValue(actModel.getActValue());
                                actModel.setActValue(value);
                            }


                            //Ha adott negyedórán belül van
                            if (actModel.getTimeLimit().compareTo(actTime) > -1 || actModel.getTimeLimit().compareTo(midnight) == 0) {
                                //TODO szum-hoz hozzáadni az előző érték integrálját

                                //TODO ha új utasítás ugyanaz, mint előző, akkor figyelmen kívül hagyjuk


                                actModel.setPrevValue(actModel.getActValue());
                                actModel.setActValue(value);
                            }
                            //Ha adott negyedórán kívül van
                            else {
                                //TODO kiírandó (előző) negyedórához hozzáadni az utolsó utasítás értékét + az új (aktuális) negyedórához hozzáadni, ha "átlógott" valamennyi az utasításból
                                //TODO fájlba kiírni
                                actModel.setTimeLimit(actModel.getTimeLimit().plusMinutes(resolution));
                            }


                        }
                        //Ha adott rekord nem kell

                    } catch (Exception e) {
                        e.printStackTrace();
                        //todo log
                    }
                }
            }
        }
    }

    private FirModel getFirModelByName(List<FirModel> firModels, String[] record, Index index) {
        if (firModels != null) {
            for (FirModel model : firModels) {
                if (model.getArnam().equals(record[index.getArnam()]) &&
                        model.getMotnam().equals(record[index.getMotnam()]) &&
                        model.getName().equals(record[index.getName()])) {
                    return model;
                }
            }
        }
        return null;
    }

    private boolean isRecordNeeded(String[] record, Index index) {
//            for (EngineParamFir param : tfrEngines) {
//                if (param.getArnam().equals(record[index.getArnam()]) &&
//                        param.getMotnam().equals(record[index.getMotnam()]) &&
//                        param.getName().equals(record[index.getName()])) {
//                    return true;
//                }
//            }
//        return false;

        String arnam = "KUP";
        String motnam = "KUP_VGM";
        String name = "GM_SETPOINT_CNTR";

        return arnam.equals(record[index.getArnam()]) && motnam.equals(record[index.getMotnam()]) && name.equals(record[index.getName()]);
    }

    private void setIndexes(String[] record, Index index) {
        int idx = 0;
        for (String actCell : record) {
            if (isArnam(actCell)) {
                index.setArnam(idx);
            } else if (isMotnam(actCell)) {
                index.setMotnam(idx);
            } else if (isName(actCell)) {
                index.setName(idx);
            } else if (isMeaVal(actCell)) {
                index.setMeaval(idx);
            } else if (isActtim(actCell)) {
                index.setActtim(idx);
            }
            idx++;
        }
    }

    private boolean isArnam(String cell) {
        return cell.trim().equalsIgnoreCase("arnam");
    }

    private boolean isMotnam(String cell) {
        return cell.trim().equalsIgnoreCase("motnam");
    }

    private boolean isName(String cell) {
        return cell.trim().equalsIgnoreCase("name");
    }

    private boolean isMeaVal(String cell) {
        return cell.trim().equalsIgnoreCase("mea_val_int");
    }

    private boolean isActtim(String cell) {
        return cell.trim().equalsIgnoreCase("acttim");
    }

    //NAS
    public void getFilesFromNas(LocalDate from, LocalDate to) {
        try {
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, Credentials.USER, Credentials.PW);
            SmbFile dir = new SmbFile(Constants.SMB + Constants.NAS_URL + Constants.DIR_FIR, auth);

            List<String> folders = getFolders(from, to);
            LocalDate fileDate;

            SmbFile[] nasFolders = dir.listFiles();

            Arrays.sort(nasFolders, ((o1, o2) -> {
                try {
                    return o1.getName().compareTo(o2.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                    //TODO log
                }
                return -1;
            }));

            SmbFile[] nasFiles;

            for (SmbFile act : nasFolders) {
                try {
//                    System.out.println(act.getName());
                    for (String folder : folders) {
                        if (act.getName().equalsIgnoreCase(folder)) {
                            nasFiles = act.listFiles();
                            for (SmbFile actFile : nasFiles) {
                                fileDate = string2LocalDate(actFile.getName().substring(13, 21));

                                if (isFirFileNeeded(actFile.getName()) && fileDate.isAfter(from) && fileDate.isBefore(to)) {
                                    //todo fájl letöltés
                                    downloadSmbFile(actFile);
                                    System.out.println(actFile.getName());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            SmbFileInputStream smbFileInputStream = new SmbFileInputStream(dir);


        } catch (MalformedURLException e) {
            e.printStackTrace();
            //TODO log
        } catch (SmbException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void downloadSmbFile(SmbFile file) {
        try {
            String name = file.getName().replace("/", "");
            FileOutputStream fileOutputStream = new FileOutputStream(name);
            InputStream fileInputStream = file.getInputStream();
            byte[] buffer = new byte[8 * 1024];

            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO log
        }
    }

    private boolean isFirFileNeeded(String name) {
        String s = name.trim().toLowerCase();
        return s.contains(Constants.CODE_ACS) || s.contains(Constants.CODE_KUP) || s.contains(Constants.CODE_PV1) || s.contains(Constants.CODE_PV2);
    }

    //HELPER

    public List<String> getFolders(LocalDate from, LocalDate to) {
        String stringFrom, stringTo;

        stringFrom = String.valueOf(from.getYear());
        stringTo = String.valueOf(to.getYear());

        if (from.getMonthValue() < 10) {
            stringFrom += "0" + String.valueOf(from.getMonthValue());
        } else {
            stringFrom += String.valueOf(from.getMonthValue());
        }

        if (to.getMonthValue() < 10) {
            stringTo += "0" + String.valueOf(to.getMonthValue());
        } else {
            stringTo += String.valueOf(to.getMonthValue());
        }

        List<String> result = new LinkedList<>();
        if (stringFrom.equalsIgnoreCase(stringTo)) {
            result.add(stringFrom + "/");
        } else {
            result.add(stringFrom + "/");
            result.add(stringTo + "/");
        }

        return result;
    }

    /**
     * Creates a java.io.Reader from a compressed file. Accepted formats: gzip, bzip2, xz, lzma, pack200, deflate and z.
     *
     * @param path Compressed file lastPath
     * @return java.io.Reader
     */
    private Reader readCompressedFile(String path) {
        try {
            FileInputStream fin = new FileInputStream(path);
            BufferedInputStream bis = new BufferedInputStream(fin);
            CompressorInputStream input = new CompressorStreamFactory().createCompressorInputStream(bis);
            return new InputStreamReader(input);
        } catch (FileNotFoundException | CompressorException e) {
            logger.stacktrace(FirSetpointController.class.getName(), "File \"" + path + "\" not found/corrupted!", e);
            return null;
        } catch (Exception e) {
            logger.stacktrace(FirSetpointController.class.getName(), "Error reading file \"" + path + "\"", e);
            return null;
        }
    }

    private LocalDate getLocalDateFromPath(String path) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String regex = "20\\d{6}";
            File file = new File(path);
            String filename = file.getName();
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                String date = filename.substring(matcher.start(), matcher.start() + 8);
                return LocalDate.parse(date, formatter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            //todo log
        }
        return null;
    }

    private LocalDate string2LocalDate(String date) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            return LocalDate.parse(date, formatter);
        } catch (Exception e) {
            e.printStackTrace();
            //todo log
        }
        return null;
    }

    private LocalDateTime string2LocalDateTime(String date) {
        DateTimeFormatter formatter;
        try {
            if (date.length() > 16) {
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
            } else {
                formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd H:mm");
            }
            return LocalDateTime.parse(date, formatter);
        } catch (Exception e) {
            e.printStackTrace();
            //todo log
        }
        return null;
    }

    private void removeQM(String[] array) {
        if (array != null) {
            int size = array.length;
            int idx = 0;

            do {
                array[idx] = array[idx].replace("\"", "");
                idx++;
            } while (idx < size);
        }
    }
}

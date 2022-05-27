package main;

import controller.FirSetpointController;
import log.Logger;

import java.time.LocalDate;

public class FirSetpointMain {
    public static void main(String[] args) {
        Logger logger = new Logger();
        FirSetpointController controller = new FirSetpointController(logger);

        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to = LocalDate.now();

        String arnam = "KUP";
        String motnam = "KUP_VGM";
        String name = "GM_SETPOINT_CNTR";


        controller.getFilesFromNas(from, to);
    }
}

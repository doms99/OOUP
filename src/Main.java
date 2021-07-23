import components.Frame;
import model.TextEditorModel;

import javax.swing.*;

public class Main {
  public static void main(String[] args) {
    TextEditorModel model = new TextEditorModel("Zločin i kazna ne može se svrstati u samo jednu kategoriju i vrstu, već ga karakteriziramo kao kriminalistički i psihološki roman.\n" +
        "Kriminalistički zato što zaista obrađuje temu ubojstva i potragu za ubojicom, ali za razliku od ostalih romana te vrste, čitatelju se ne otkrivaju tragovi koji \n" +
        "će postepeno dovesti do ubojice, već nam je počinitelj poznat od samog početka, a pratimo njegovo psihički stanje nakon ubojstva.\n" +
        "Međutim, inspektor Porfirij Petrovič ne zna tko je ubojica pa pratimo i njegovu istragu te postepeno sužavanje kruga oko Raskoljnikova.\n" +
        "Doduše, možda bi ipak bilo točnije kategorizirati ovaj roman kao psihološki, s obzirom na to da dobivamo uvid u psihičko stanje \n" +
        "glavnog lika kroz devet dana, od ubojstva lihvarice do njegove predaje policiji.");
    SwingUtilities.invokeLater(() -> {
      new Frame(model).setVisible(true);
    });
  }
}

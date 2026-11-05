# Activity-scoped save state is instantiated by ViewModelProvider through its public constructor.
-keepclassmembers class com.gkemon.XMLtoPDF.PdfGenerator$PendingSave {
    public <init>();
}

package edu.harvard.iq.dataverse.util;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldValue;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

/**
 * A means of iterating over {@link DatasetField}s, or a collection of them.
 * As these may have a complex structure (compound values, etc), this object
 * allows processing them via an event stream, similar to SAX parsing of XML.
 * Visiting of the fields is done in display order.
 *
 * @author michael
 */
public class DatasetFieldWalker {

    private static final Logger logger = Logger.getLogger(DatasetFieldWalker.class.getCanonicalName());

    public interface Listener {
        void startField(DatasetField f);

        void endField(DatasetField f);

        void primitiveValue(DatasetFieldValue dsfv);

        void controledVocabularyValue(ControlledVocabularyValue cvv);

        void startCompoundValue(DatasetFieldCompoundValue dsfcv);

        void endCompoundValue(DatasetFieldCompoundValue dsfcv);
    }

    /**
     * Convenience method to walk over a field.
     *
     * @param dsf the field to walk over.
     * @param l   the listener to execute on {@code dsf}'s values and structure.
     */
    public static void walk(DatasetField dsf, Listener l) {
        DatasetFieldWalker joe = new DatasetFieldWalker(l);
        joe.walk(dsf, true);
    }

    /**
     * Convenience method to walk over a list of fields. Traversal
     * is done in display order.
     *
     * @param fields  the fields to go over. Does not have to be sorted.
     * @param excludeEmailFields is email excluded from export
     * @param l       the listener to execute on each field values and structure.
     */
    public static void walk(List<DatasetField> fields, Listener l, boolean excludeEmailFields) {
        DatasetFieldWalker joe = new DatasetFieldWalker(l);
        for (DatasetField dsf : sort(fields, DatasetField.DisplayOrder)) {
            joe.walk(dsf, excludeEmailFields);
        }
    }

    private Listener l;


    public DatasetFieldWalker(Listener l) {
        this.l = l;
    }

    public DatasetFieldWalker() {
        this(null);
    }

    public void walk(DatasetField fld, boolean excludeEmailFields) {
        l.startField(fld);
        DatasetFieldType datasetFieldType = fld.getDatasetFieldType();

        if (datasetFieldType.isControlledVocabulary()) {
            for (ControlledVocabularyValue cvv
                    : sort(fld.getControlledVocabularyValues(), ControlledVocabularyValue.DisplayOrder)) {
                l.controledVocabularyValue(cvv);
            }

        } else if (datasetFieldType.isPrimitive()) {
            for (DatasetFieldValue pv : sort(fld.getDatasetFieldValues(), DatasetFieldValue.DisplayOrder)) {
                if (excludeEmailFields && FieldType.EMAIL.equals(pv.getDatasetField().getDatasetFieldType().getFieldType())) {
                    continue;
                }
                l.primitiveValue(pv);
            }

        } else if (datasetFieldType.isCompound()) {
            for (DatasetFieldCompoundValue dsfcv : sort(fld.getDatasetFieldCompoundValues(), DatasetFieldCompoundValue.DisplayOrder)) {
                l.startCompoundValue(dsfcv);
                for (DatasetField dsf : sort(dsfcv.getChildDatasetFields(), DatasetField.DisplayOrder)) {
                    walk(dsf, excludeEmailFields);
                }
                l.endCompoundValue(dsfcv);
            }
        }
        l.endField(fld);
    }


    public void setL(Listener l) {
        this.l = l;
    }

    static private <T> Iterable<T> sort(List<T> list, Comparator<T> cmp) {
        ArrayList<T> tbs = new ArrayList<>(list);
        Collections.sort(tbs, cmp);
        return tbs;
    }

}

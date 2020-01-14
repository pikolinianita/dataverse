package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.MarkupChecker;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author skraffmiller
 */
@Entity
@ValidateDatasetFieldType
@Table(indexes = {@Index(columnList = "datasetfieldtype_id"), @Index(columnList = "datasetversion_id"),
        @Index(columnList = "parentdatasetfieldcompoundvalue_id"), @Index(columnList = "template_id")})
public class DatasetField implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NA_VALUE = "N/A";

    /**
     * Orders dataset fields by their display order.
     */
    public static final Comparator<DatasetField> DisplayOrder = new Comparator<DatasetField>() {
        @Override
        public int compare(DatasetField o1, DatasetField o2) {
            return Integer.compare(o1.getDatasetFieldType().getDisplayOrder(),
                                   o2.getDatasetFieldType().getDisplayOrder());
        }
    };

    public static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType, Object dsv) {

        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        //TODO - a better way to handle this?
        if (dsv instanceof DatasetVersion) {
            dsfv.setDatasetVersion((DatasetVersion) dsv);
        } else if (dsv instanceof Template) {
            dsfv.setTemplate((Template) dsv);
        }

        return dsfv;
    }

    // originally this was an overloaded method, but we renamed it to get around an issue with Bean Validation
    // (that looked t overloaded methods, when it meant to look at overriden methods
    public static DatasetField createNewEmptyChildDatasetField(DatasetFieldType dsfType, DatasetField compoundValue) {
        DatasetField dsfv = createNewEmptyDatasetField(dsfType);
        dsfv.setDatasetFieldParent(compoundValue);
        return dsfv;
    }

    private static DatasetField createNewEmptyDatasetField(DatasetFieldType dsfType) {
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(dsfType);

        if (dsfType.isCompound() || dsfType.isControlledVocabulary()) {
            dsf.getDatasetFieldsChildren().add(createNewEmptyDatasetFieldCompoundValue(dsf));
        }

        return dsf;

    }

    public static DatasetField createNewEmptyDatasetFieldCompoundValue(DatasetField dsf) {
        DatasetField compoundValue = new DatasetField();
        compoundValue.setDatasetFieldParent(dsf);

        for (DatasetFieldType dsfType : dsf.getDatasetFieldType().getChildDatasetFieldTypes()) {
            compoundValue.getDatasetFieldsChildren().add(DatasetField.createNewEmptyChildDatasetField(dsfType,
                                                                                                      compoundValue));
        }

        return compoundValue;
    }

    /**
     * Groups a list of fields by the block they belong to.
     *
     * @param fields well, duh.
     * @return a map, mapping each block to the fields that belong to it.
     */
    public static Map<MetadataBlock, List<DatasetField>> groupByBlock(List<DatasetField> fields) {
        Map<MetadataBlock, List<DatasetField>> retVal = new HashMap<>();
        for (DatasetField f : fields) {
            MetadataBlock metadataBlock = f.getDatasetFieldType().getMetadataBlock();
            List<DatasetField> lst = retVal.get(metadataBlock);
            if (lst == null) {
                retVal.put(metadataBlock, new LinkedList<>(Collections.singleton(f)));
            } else {
                lst.add(f);
            }
        }
        return retVal;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(nullable = false)
    private DatasetFieldType datasetFieldType;

    public DatasetFieldType getDatasetFieldType() {
        return datasetFieldType;
    }

    public void setDatasetFieldType(DatasetFieldType datasetField) {
        this.datasetFieldType = datasetField;
    }

    @ManyToOne
    private DatasetVersion datasetVersion;

    public DatasetVersion getDatasetVersion() {
        return datasetVersion;
    }

    public void setDatasetVersion(DatasetVersion datasetVersion) {
        this.datasetVersion = datasetVersion;
    }

    @ManyToOne
    private Template template;

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    @OneToMany(mappedBy = "datasetFieldParent", orphanRemoval = true, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @OrderBy("displayOrder ASC")
    private List<DatasetField> datasetFieldsChildren = new ArrayList<>();

    public List<DatasetField> getDatasetFieldsChildren() {
        return datasetFieldsChildren;
    }

    public void setDatasetFieldsChildren(List<DatasetField> compundDatasetFields) {
        this.datasetFieldsChildren = compundDatasetFields;
    }

    @ManyToOne
    @JoinColumn
    private DatasetField datasetFieldParent;

    public Option<DatasetField> getDatasetFieldParent() {
        return Option.of(datasetFieldParent);
    }

    public DatasetField setDatasetFieldParent(DatasetField datasetFieldParent) {
        this.datasetFieldParent = datasetFieldParent;
        return this;
    }

    @Column(columnDefinition = "TEXT")
    private String fieldValue;

    public Option<String> getFieldValue() {
        return Option.of(fieldValue);
    }

    public DatasetField setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
        return this;
    }

    private int displayOrder;

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    @ManyToMany(cascade = {CascadeType.MERGE})
    @JoinTable(indexes = {@Index(columnList = "datasetfield_id"), @Index(columnList = "controlledvocabularyvalues_id")})
    private List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList<>();

    public List<ControlledVocabularyValue> getControlledVocabularyValues() {
        return controlledVocabularyValues;
    }

    public void setControlledVocabularyValues(List<ControlledVocabularyValue> controlledVocabularyValues) {
        this.controlledVocabularyValues = controlledVocabularyValues;
    }

    public ControlledVocabularyValue getSingleControlledVocabularyValue() {
        if (!controlledVocabularyValues.isEmpty()) {
            return controlledVocabularyValues.get(0);
        } else {
            return null;
        }
    }

    public void setSingleControlledVocabularyValue(ControlledVocabularyValue cvv) {
        if (!controlledVocabularyValues.isEmpty()) {
            controlledVocabularyValues.set(0, cvv);
        } else {
            controlledVocabularyValues.add(cvv);
        }
    }

    public String getValue() {
        if (!getFieldValue().isEmpty()) {
            return fieldValue;
        } else if (controlledVocabularyValues != null && !controlledVocabularyValues.isEmpty()) {
            if (controlledVocabularyValues.get(0) != null) {
                return controlledVocabularyValues.get(0).getStrValue();
            }
        }
        return null;
    }

    public String getDisplayValue() {
        String returnString = "";
        for (String value : getValues()) {
            if (value == null) {
                value = "";
            }
            returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
        }
        return returnString;
    }

    public String getRawValue() {
        String returnString = "";
        for (String value : getRawValuesList()) {
            if (value == null) {
                value = "";
            }
            returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
        }
        return returnString;
    }

    public String getCompoundDisplayValue() {

        return datasetFieldsChildren.stream()
                .filter(datasetField -> datasetField.getFieldValue().isDefined())
                .map(datasetField -> datasetField.fieldValue)
                .map(String::trim)
                .collect(Collectors.joining("; "));
    }

    public String getCompoundRawValue() {
        String returnString = "";
        for (DatasetField dsf : getDatasetFieldsChildren()) {
            for (String value : dsf.getRawValuesList()) {
                if (!(value == null)) {
                    returnString += (returnString.isEmpty() ? "" : "; ") + value.trim();
                }
            }
        }
        return returnString;
    }

    /**
     * despite the name, this returns a list of display values; not a list of values
     */
    public List<String> getValues() {
        List<String> returnList = new ArrayList<>();
        if (!getFieldValue().isEmpty()) {
            returnList.add(getFieldDisplayValue());
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getLocaleStrValue() != null) {
                    returnList.add(cvv.getLocaleStrValue());
                }
            }
        }
        return returnList;
    }

    public List<String> getRawValuesList() {
        List<String> returnList = new ArrayList<>();
        if (!getDatasetFieldsChildren().isEmpty()) {
            for (DatasetField dsf : getDatasetFieldsChildren()) {
                returnList.add(dsf.getUnsanitizedDisplayValue());
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getStrValue() != null) {
                    returnList.add(cvv.getStrValue());
                }
            }
        }
        return returnList;
    }

    /**
     * list of values (as opposed to display values).
     * used for passing to solr for indexing
     */
    public List<String> getValues_nondisplay() {
        List returnList = new ArrayList();

        if (getFieldValue().isDefined()){
            returnList.add(fieldValue);
        }

        if (!getDatasetFieldsChildren().isEmpty()) {
            for (DatasetField dsf : getDatasetFieldsChildren()) {
                String value = dsf.getValue();
                if (value != null) {
                    returnList.add(value);
                }
            }
        } else {
            for (ControlledVocabularyValue cvv : controlledVocabularyValues) {
                if (cvv != null && cvv.getStrValue() != null) {
                    returnList.add(cvv.getStrValue());
                }
            }
        }
        return returnList;
    }

    /**
     * appears to be only used for sending info to solr; changed to return values
     * instead of display values
     */
    public List<String> getValuesWithoutNaValues() {
        List<String> returnList = getValues_nondisplay();
        returnList.removeAll(Arrays.asList(NA_VALUE));
        return returnList;
    }

    public String getFieldDisplayValue() { //dsf value
        String retVal = "";
        if (!StringUtils.isBlank(this.getValue()) && !DatasetField.NA_VALUE.equals(this.getValue())) {
            String format = getDatasetFieldType().getDisplayFormat();
            if (StringUtils.isBlank(format)) {
                format = "#VALUE";
            }
            String sanitizedValue = !getDatasetFieldType().isSanitizeHtml() ?
                    this.getValue() :
                    MarkupChecker.sanitizeBasicHTML(this.getValue());

            if (!getDatasetFieldType().isSanitizeHtml() && getDatasetFieldType().isEscapeOutputText()) {
                sanitizedValue = MarkupChecker.stripAllTags(sanitizedValue);
            }

            // replace the special values in the format (note: we replace #VALUE last since we don't
            // want any issues if the value itself has #NAME in it)
            String displayValue = format
                    .replace("#NAME", getDatasetFieldType().getTitle() == null ? "" : getDatasetFieldType().getTitle())
                    .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                    .replace("#VALUE", sanitizedValue);
            retVal = displayValue;
        }

        return retVal;
    }

    public String getUnsanitizedDisplayValue() {
        String retVal = "";
        if (!StringUtils.isBlank(this.getValue()) && !DatasetField.NA_VALUE.equals(this.getValue())) {
            String format = getDatasetFieldType().getDisplayFormat();
            if (StringUtils.isBlank(format)) {
                format = "#VALUE";
            }
            String value = this.getValue();
            String displayValue = format
                    .replace("#NAME", getDatasetFieldType().getTitle() == null ? "" : getDatasetFieldType().getTitle())
                    .replace("#EMAIL", BundleUtil.getStringFromBundle("dataset.email.hiddenMessage"))
                    .replace("#VALUE", value);
            retVal = displayValue;
        }
        return retVal;
    }

    public boolean isEmpty() {
        return isEmpty(false);
    }

    public boolean isEmptyForDisplay() {
        return isEmpty(true);
    }


    private boolean isEmpty(boolean forDisplay) {
        if (datasetFieldType.isPrimitive()) { // primitive
            List<String> values = forDisplay ? getValues() : getValues_nondisplay();
            for (String value : values) {
                if (!StringUtils.isBlank(value) && !(forDisplay && DatasetField.NA_VALUE.equals(value))) {
                    return false;
                }
            }
        } else { // compound
            for (DatasetField subField : getDatasetFieldsChildren()) {
                if (!subField.isEmpty(forDisplay)) {
                    return false;
                }
            }

        }

        return true;
    }

    @Transient
    private String validationMessage;

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    @Transient
    private Boolean required;

    public boolean isRequired() {
        if (required == null) {
            required = this.datasetFieldType.isPrimitive() && this.datasetFieldType.isRequired();

            if (this.datasetFieldType.isHasRequiredChildren()) {
                required = true;
            }

            Dataverse dv = getDataverse();
            while (!dv.isMetadataBlockRoot()) {
                if (dv.getOwner() == null) {
                    break; // we are at the root; which by defintion is metadata blcok root, regarldess of the value
                }
                dv = dv.getOwner();
            }

            List<DataverseFieldTypeInputLevel> dftilListFirst = dv.getDataverseFieldTypeInputLevels();
            if (!getDatasetFieldType().isHasChildren()) {
                for (DataverseFieldTypeInputLevel dsftil : dftilListFirst) {
                    if (dsftil.getDatasetFieldType().equals(this.datasetFieldType)) {
                        required = dsftil.isRequired();
                    }
                }
            }

            if (getDatasetFieldType().isHasChildren() && (!dftilListFirst.isEmpty())) {
                for (DatasetFieldType child : getDatasetFieldType().getChildDatasetFieldTypes()) {
                    for (DataverseFieldTypeInputLevel dftilTest : dftilListFirst) {
                        if (child.equals(dftilTest.getDatasetFieldType())) {
                            if (dftilTest.isRequired()) {
                                required = true;
                            }
                        }
                    }
                }
            }

        }
        // logger.fine("at return  " + this.datasetFieldType.getDisplayName() + " " + required);
        return required;
    }

    public Dataverse getDataverse() {
        if (datasetVersion != null) {
            return datasetVersion.getDataset().getOwner();
        } else if (datasetFieldParent != null) {
            return datasetFieldParent.getDataverse();
        } else if (template != null) {
            return template.getDataverse();
        } else {
            throw new IllegalStateException(
                    "DatasetField is in an illegal state: no dataset version, compound value, or template is set as its parent.");
        }
    }


    @Transient
    private boolean include;

    public void setInclude(boolean include) {
        this.include = include;
    }

    /**
     * Returns true if datasetField should be shown
     * on edit metadata form (when creating/editing dataset or template).
     */
    public boolean isInclude() {
        return this.include;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DatasetField)) {
            return false;
        }
        DatasetField other = (DatasetField) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "DatasetField[ id=" + id + " ]";
    }

    public DatasetField copy() {
        DatasetField dsf = new DatasetField();
        dsf.setDatasetFieldType(datasetFieldType);
        dsf.setControlledVocabularyValues(controlledVocabularyValues);

        for (DatasetField dsfChildren : getDatasetFieldsChildren()) {
            dsfChildren.setDatasetFieldParent(dsf);
            dsf.getDatasetFieldsChildren().add(dsfChildren.copy());
        }

        return dsf;
    }

    public boolean removeBlankDatasetFieldValues() {
        if (this.getDatasetFieldType().isPrimitive()) {
            if (this.getDatasetFieldType().isControlledVocabulary()) {
                return this.getControlledVocabularyValues().isEmpty();
            } else if (this.getDatasetFieldType().isCompound()) {
                Iterator<DatasetField> cvIt = this.getDatasetFieldsChildren().iterator();
                while (cvIt.hasNext()) {
                    DatasetField cv = cvIt.next();
                    cv.getDatasetFieldsChildren().removeIf(DatasetField::removeBlankDatasetFieldValues);
                    if (cv.getDatasetFieldsChildren().isEmpty()) {
                        cvIt.remove();
                    }
                }
                return this.getDatasetFieldsChildren().isEmpty();
            }
            return false;
        }
        return false;
    }

    public void setValueDisplayOrder() {

        if (this.getDatasetFieldType().isPrimitive() && !this.getDatasetFieldType().isControlledVocabulary()) {
            for (int i = 0; i < getDatasetFieldsChildren().size(); i++) {
                datasetFieldsChildren.get(i).setDisplayOrder(i);
            }

        } else if (this.getDatasetFieldType().isCompound()) {
            for (int i = 0; i < getDatasetFieldsChildren().size(); i++) {
                DatasetField compoundValue = getDatasetFieldsChildren().get(i);
                compoundValue.setDisplayOrder(i);
                compoundValue.setValueDisplayOrder();
            }
        }
    }


    public void trimTrailingSpaces() {

        if (this.getDatasetFieldType().isPrimitive() && !this.getDatasetFieldType().isControlledVocabulary()) {
            for (DatasetField datasetFieldValue : getDatasetFieldsChildren()) {
                datasetFieldValue.setFieldValue(datasetFieldValue.getValue().trim());
            }
        } else if (this.getDatasetFieldType().isCompound()) {
            for (DatasetField dsf : getDatasetFieldsChildren()) {
                dsf.trimTrailingSpaces();
            }
        }
    }


    /**
     * If this is a FieldType.TEXT or FieldType.TEXTBOX, then run it through the markup checker
     *
     * @return
     */
    public boolean needsTextCleaning() {


        if (this.getDatasetFieldType() == null || this.getDatasetFieldType().getFieldType() == null) {
            return false;
        }

        if (this.datasetFieldType.getFieldType().equals(FieldType.TEXT)) {
            return true;
        } else {
            return this.datasetFieldType.getFieldType().equals(FieldType.TEXTBOX);
        }

    } // end: needsTextCleaning


}

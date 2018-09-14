package allen.free.xml;

import allen.free.xml.annotation.XmlAttribute;
import allen.free.xml.annotation.XmlElement;
import allen.free.xml.annotation.XmlElementWrapper;
import allen.free.xml.annotation.XmlRootElement;
import allen.free.xml.annotation.XmlType;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class XmlGenerator {

  public String generateXml(Object object, Class clazz) {
    StringBuilder stringBuilder = new StringBuilder();
    if (!clazz.isAnnotationPresent(XmlRootElement.class)) {
      return stringBuilder.toString();
    }

    try {
      return buildRootXml(object, clazz);
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return stringBuilder.toString();
  }


  private String buildRootXml(Object object, Class clazz)
      throws NoSuchFieldException, IllegalAccessException {
    StringBuilder stringBuilder = new StringBuilder();
    String elementName = clazz.getSimpleName();
    XmlRootElement xmlRootElement = (XmlRootElement) clazz.getAnnotation(XmlRootElement.class);
    if (isNotNullAndEmpty(xmlRootElement.name())) {
      elementName = xmlRootElement.name();
    }
    boolean selfClosing = xmlRootElement.selfClosing();
    List<Field> attributeFieldList = buildAttributeList(clazz);
    List<Field> elementFieldList = buildElementList(clazz);

    String attributeBuilder =  buildAttributeListXml(object, attributeFieldList);

    String fieldBuilder =  buildFieldListXml(object, elementFieldList);



    stringBuilder.append(buildElementXmlWithoutWrapper(elementName,false,selfClosing,attributeBuilder,fieldBuilder));
    return stringBuilder.toString();
  }

  private String buildFieldListXml(Object object, List<Field> elementFieldList
  ) throws IllegalAccessException, NoSuchFieldException {
    StringBuilder fieldBuilder = new StringBuilder();
    for (Field elementField : elementFieldList) {
      elementField.setAccessible(true);
      StringBuilder fieldBuilderWithoutWrapper = new StringBuilder();
      if (elementField.get(object) instanceof List) {
        List elementFieldValue = (List) elementField.get(object);
        Type type = elementField.getGenericType();
        if (type instanceof ParameterizedType) {
          for (int i = 0; i < elementFieldValue.size(); i++) {
            fieldBuilderWithoutWrapper
                .append(buildFieldXml(elementField, elementFieldValue.get(i)));
          }
        }
      } else {
        fieldBuilderWithoutWrapper.append(buildFieldXml(elementField, elementField.get(object)));
      }

      String wrapperName = "";
      boolean wrapperIgnoreWhenEmpty = true;
      boolean wrapperSelfClosing = true;
      if (elementField.isAnnotationPresent(XmlElementWrapper.class)) {
        XmlElementWrapper xmlElementWrapper = elementField.getAnnotation(XmlElementWrapper.class);
        wrapperName = xmlElementWrapper.name();

        wrapperIgnoreWhenEmpty = xmlElementWrapper.ignoreWhenNullOrEmpty();
        wrapperSelfClosing = xmlElementWrapper.selfClosing();
      }
      if (isNotNullAndEmpty(wrapperName)) {
        fieldBuilder.append(buildElementXml(wrapperName, wrapperIgnoreWhenEmpty, wrapperSelfClosing,
            fieldBuilderWithoutWrapper.toString()));
      } else {
        fieldBuilder.append(fieldBuilderWithoutWrapper.toString());
      }

    }
    return fieldBuilder.toString();
  }


  private String buildFieldXml(Field field, Object fieldValue)
      throws NoSuchFieldException, IllegalAccessException {
    StringBuilder stringBuilder = new StringBuilder();
    Type type = field.getGenericType();
    Class clazz = field.getType();
    if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      clazz = (Class) pt.getActualTypeArguments()[0]; //
    }
    if (!field.isAnnotationPresent(XmlElement.class)) {
      return stringBuilder.toString();
    }

    StringBuilder fileXmlBuilder = new StringBuilder();
    XmlElement xmlElement = field.getAnnotation(XmlElement.class);
    String elementName = field.getName();
    String name = xmlElement.name();
    if (isNotNullAndEmpty(name)) {
      elementName = name;
    }
    boolean ignoreWhenEmpty = xmlElement.ignoreWhenNullOrEmpty();
    boolean selfClosing = xmlElement.selfClosing();
    if (isWrapperClassOrString(clazz) || fieldValue == null) {
      if (fieldValue == null) {
        fieldValue = "";
      }
      fileXmlBuilder.append(
          buildElementXml(elementName, ignoreWhenEmpty, selfClosing, fieldValue.toString()));

      stringBuilder.append(fileXmlBuilder.toString());

      return stringBuilder.toString();
    }

    List<Field> attributeFieldList =  buildAttributeList(clazz);
    List<Field> elementFieldList = buildElementList(clazz);


    String attributeBuilder = buildAttributeListXml(fieldValue, attributeFieldList);

    String fieldBuilder =  buildFieldListXml(fieldValue, elementFieldList);


    String elementXmlWithoutWrapper = buildElementXmlWithoutWrapper(elementName, ignoreWhenEmpty,
        selfClosing,
        attributeBuilder, fieldBuilder);

    stringBuilder.append(elementXmlWithoutWrapper);

    return stringBuilder.toString();

  }

  private String buildElementXmlWithoutWrapper(String elementName,
      boolean ignoreWhenEmpty, boolean selfClosing, String attributeBuilder,
      String fieldBuilder) {
    StringBuilder stringBuilder = new StringBuilder();
    if (attributeBuilder.length() == 0 && fieldBuilder.length() == 0) {
      if (ignoreWhenEmpty) {
        return stringBuilder.toString();
      }
      if (selfClosing) {
        stringBuilder.append("<" + elementName + "/>");
      } else {
        stringBuilder.append("<" + elementName + ">");
        stringBuilder.append("</" + elementName + ">");
      }
      return stringBuilder.toString();
    }

    if (attributeBuilder.length() > 0 && fieldBuilder.length() == 0) {
      if (selfClosing) {
        stringBuilder.append("<" + elementName + attributeBuilder.toString() + "/>");
      } else {
        stringBuilder.append("<" + elementName + attributeBuilder.toString() + ">");
        stringBuilder.append("</" + elementName + ">");
      }
    }

    if (attributeBuilder.length() > 0 && fieldBuilder.length() > 0) {
      stringBuilder.append("<" + elementName + attributeBuilder.toString() + ">");
      stringBuilder.append(fieldBuilder.toString());
      stringBuilder.append("</" + elementName + ">");
    }

    if (attributeBuilder.length() == 0 && fieldBuilder.length() > 0) {
      stringBuilder.append("<" + elementName + ">");
      stringBuilder.append(fieldBuilder.toString());
      stringBuilder.append("</" + elementName + ">");
    }
    return stringBuilder.toString();
  }

  private String buildAttributeListXml(Object fieldValue, List<Field> attributeFieldList) throws IllegalAccessException {
    StringBuilder attributeBuilder = new StringBuilder();
    for (Field attributeField : attributeFieldList) {
      if (!isWrapperClassOrString(attributeField.getType())) {

      }
      String attributeName = attributeField.getName();
      XmlAttribute xmlAttribute = attributeField.getAnnotation(XmlAttribute.class);
      String attName = xmlAttribute.name();
      if (isNotNullAndEmpty(attName)) {
        attributeName = attName;
      }
      boolean attributeIgnoreWhenEmpty = xmlAttribute.ignoreWhenNullOrEmpty();
      attributeField.setAccessible(true);
      String attributeXml = buildAttributeXml(attributeName, attributeField.get(fieldValue),
          attributeIgnoreWhenEmpty);
      if (attributeXml.length() > 0) {
        attributeBuilder.append(" ");
      }
      attributeBuilder.append(attributeXml);
    }
    return attributeBuilder.toString();
  }

  private List<Field> buildElementList(Class clazz) throws NoSuchFieldException {
    List<Field> elementFieldList = new ArrayList<>();
    List<Field> fieldList = getAllField(clazz);
    if (clazz.isAnnotationPresent(XmlType.class)) {
      XmlType xmlType = (XmlType) clazz.getAnnotation(XmlType.class);
      String[] props = xmlType.propOrder();
      for (String fieldName : props) {
        Field subField = clazz.getDeclaredField(fieldName);
        if (subField.isAnnotationPresent(XmlElement.class)) {
          elementFieldList.add(subField);
        }
      }
    } else {
      for (Field subField : fieldList) {
        if (subField.isAnnotationPresent(XmlElement.class)) {
          elementFieldList.add(subField);
        }
      }
    }
    return elementFieldList;
  }

  private List<Field> buildAttributeList(Class clazz){
    List<Field> fieldList = getAllField(clazz);
    List<Field> attributeFieldList = new ArrayList<>();
    for (Field subField : fieldList) {
      if (subField.isAnnotationPresent(XmlAttribute.class)) {
        attributeFieldList.add(subField);
      }
    }
    return attributeFieldList;
  }

  private String buildAttributeXml(String attributeName, Object object, boolean ignoreWhenEmpty) {
    if (object == null) {
      if (ignoreWhenEmpty) {
        return "";
      } else {
        return attributeName + "=\"\"";
      }
    }
    return attributeName + "=\"" + object.toString() + "\"";
  }


  private List<Field> getAllField(Class clazz) {
    List<Field> fieldList = new ArrayList<>();
    Class tempClass = clazz;
    while (tempClass != null) {
      fieldList.addAll(Arrays.asList(tempClass.getDeclaredFields()));
      tempClass = tempClass.getSuperclass();
    }
    return fieldList;
  }


  private String buildElementXml(String elementName,
      boolean ignoreWhenEmpty, boolean selfClosing, String fieldXml) {
    StringBuilder stringBuilder = new StringBuilder();
    if (ignoreWhenEmpty && !isNotNullAndEmpty(fieldXml)) {
      return stringBuilder.toString();
    }
    if (selfClosing && !isNotNullAndEmpty(fieldXml)) {
      stringBuilder.append("<" + elementName + "/> ");
    } else {
      stringBuilder.append("<" + elementName + ">" + fieldXml + "</" + elementName + ">");
    }
    return stringBuilder.toString();
  }


  private boolean isNotNullAndEmpty(String str) {
    return str != null && !"".equals(str);
  }

  private boolean isWrapperClassOrString(Class clazz) {
    if (clazz.isPrimitive() || clazz == String.class) {
      return true;
    }
    try {
      return ((Class) clazz.getDeclaredField("TYPE").get(null)).isPrimitive();
    } catch (IllegalAccessException | NoSuchFieldException e) {

    }
    return false;
  }

}

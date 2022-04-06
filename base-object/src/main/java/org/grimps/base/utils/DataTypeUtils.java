/*
 * Copyright 2017 Shekhar Jha
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grimps.base.utils;

import org.grimps.base.ValidationException;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.sql.Clob;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataTypeUtils {

    private static XLogger logger = XLoggerFactory.getXLogger(DataTypeUtils.class);

    public DATA_TYPE getDataType(Object objectValue) {
        logger.entry(objectValue);
        DATA_TYPE objectType = DATA_TYPE.UNKNOWN;
        if (objectValue != null) {
            if (objectValue.getClass().equals(Boolean.class)) {
                objectType = DATA_TYPE.Boolean;
            } else if (objectValue.getClass().equals(Date.class)) {
                objectType = DATA_TYPE.Date;
            } else if (objectValue.getClass().equals(Integer.class)) {
                objectType = DATA_TYPE.Integer;
            } else if (objectValue.getClass().equals(Double.class)) {
                objectType = DATA_TYPE.Double;
            } else if (objectValue.getClass().equals(Long.class)) {
                objectType = DATA_TYPE.Long;
            } else if (objectValue.getClass().equals(Short.class)) {
                objectType = DATA_TYPE.Short;
            } else if (objectValue.getClass().equals(String.class)) {
                objectType = DATA_TYPE.String;
            }
        }
        return logger.exit(objectType);
    }

    public <I, O> O transformObjectDataType(I input, String transformedToDataType, String format) {
        logger.entry(input, transformedToDataType, format);
        Object transformedData = null;
        if (input != null) {
            DATA_TYPE inputDataType = getDataType(input);
            logger.trace("Input data type {}", inputDataType);
            DATA_TYPE data_type = DATA_TYPE.valueOfString(transformedToDataType);
            logger.trace("Data type {}", data_type);
            if (data_type != DATA_TYPE.UNKNOWN && inputDataType != DATA_TYPE.UNKNOWN) {
                switch (data_type) {
                    case Boolean:
                        switch (inputDataType) {
                            case Boolean:
                                transformedData = input;
                                break;
                            case Byte:
                                if (((Byte) input).intValue() == 1)
                                    transformedData = Boolean.TRUE;
                                else
                                    transformedData = Boolean.FALSE;
                                break;
                            case ByteArray:
                            case Clob:
                            case Date:
                                transformedData = Boolean.FALSE;
                                break;
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                if (((Number) input).shortValue() == 1)
                                    transformedData = Boolean.TRUE;
                                else
                                    transformedData = Boolean.FALSE;
                                break;
                            case String:
                                transformedData = Boolean.parseBoolean((String) input);
                                break;
                        }
                        break;
                    case Byte:
                        switch (inputDataType) {
                            case Boolean:
                                if (Boolean.TRUE == input)
                                    transformedData = (byte) 1;
                                else
                                    transformedData = (byte) 0;
                                break;
                            case Byte:
                                transformedData = input;
                                break;
                            case ByteArray:
                            case Clob:
                            case Date:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = ((Number) input).byteValue();
                                break;
                            case String:
                                transformedData = Byte.parseByte((String) input);
                                break;
                        }
                        break;
                    case ByteArray:
                        switch (inputDataType) {
                            case Boolean:
                                if (Boolean.TRUE == input)
                                    transformedData = new byte[]{1};
                                else
                                    transformedData = new byte[]{0};
                                break;
                            case ByteArray:
                                transformedData = input;
                                break;
                            case Clob:
                                try {
                                    int dataSize = (int) ((Clob) input).length();
                                    if (dataSize > 0) {
                                        String data = ((Clob) input).getSubString(1, dataSize);
                                        if (data != null) {
                                            transformedData = data.getBytes();
                                        }
                                    }
                                } catch (SQLException exception) {
                                    logger.catching(exception);
                                } finally {
                                    try {
                                        ((Clob) input).free();
                                    } catch (Exception exception1) {
                                        logger.catching(exception1);
                                    }
                                }
                                break;
                            case Byte:
                            case Date:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = ((Number) input).byteValue();
                                break;
                            case String:
                                transformedData = Byte.parseByte((String) input);
                                break;
                        }
                        break;
                    case Clob:
                        switch (inputDataType) {
                            case Boolean:
                            case Byte:
                            case ByteArray:
                            case Date:
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                            case String:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Clob:
                                transformedData = input;
                        }
                        break;
                    case Date:
                        switch (inputDataType) {
                            case Boolean:
                            case Byte:
                            case ByteArray:
                            case Clob:
                            case Short:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Date:
                                transformedData = input;
                                break;
                            case Double:
                            case Long:
                            case Integer:
                                transformedData = new Date(((Number) input).longValue());
                                break;
                            case String:
                                if (Utils.isEmpty(format)) {
                                    format = "yyyy-MM-dd'T'HH:mm:ssZ";
                                }
                                try {
                                    transformedData = new SimpleDateFormat(format).parse((String) input);
                                } catch (ParseException exception) {
                                    throw logger.throwing(new ValidationException("Failed to parse " + input + " as date using date format " + format, exception));
                                }
                                break;
                        }
                        break;
                    case Double:
                        switch (inputDataType) {
                            case Boolean:
                            case Byte:
                            case ByteArray:
                            case Clob:
                            case Date:
                                throw new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported.");
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = ((Number) input).doubleValue();
                                break;
                            case String:
                                transformedData = Double.parseDouble((String) input);
                                break;
                        }
                        break;
                    case Integer:
                        switch (inputDataType) {
                            case Boolean:
                            case Byte:
                            case ByteArray:
                            case Clob:
                            case Date:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = ((Number) input).intValue();
                                break;
                            case String:
                                transformedData = Integer.parseInt((String) input);
                                break;
                        }
                        break;
                    case Long:
                        switch (inputDataType) {
                            case Boolean:
                            case Byte:
                            case ByteArray:
                            case Clob:
                            case Date:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = ((Number) input).longValue();
                                break;
                            case String:
                                transformedData = Long.parseLong((String) input);
                                break;
                        }
                        break;
                    case Short:
                        switch (inputDataType) {
                            case Boolean:
                            case Byte:
                            case ByteArray:
                            case Clob:
                            case Date:
                                throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = ((Number) input).shortValue();
                                break;
                            case String:
                                transformedData = Short.parseShort((String) input);
                                break;
                        }
                        break;
                    case String:
                        switch (inputDataType) {
                            case Boolean:
                                transformedData = input.toString();
                                break;
                            case Byte:
                                transformedData = input.toString();
                                break;
                            case ByteArray:
                                transformedData = new String((byte[]) input);
                                break;
                            case Clob:
                                try {
                                    int dataSize = (int) ((Clob) input).length();
                                    if (dataSize > 0) {
                                        transformedData = ((Clob) input).getSubString(1, dataSize);
                                    }
                                } catch (SQLException exception) {
                                    throw logger.throwing(new ValidationException("Failed to extract text from clob", exception));
                                } finally {
                                    try {
                                        ((Clob) input).free();
                                    } catch (Exception exception1) {
                                        logger.catching(exception1);
                                    }
                                }
                                break;
                            case Date:
                            case Double:
                            case Integer:
                            case Long:
                            case Short:
                                transformedData = input.toString();
                                break;
                            case String:
                                transformedData = input;
                                break;
                        }
                        break;
                    default:
                        throw logger.throwing(new ValidationException("Transformation from " + inputDataType + " to " + data_type + " is not supported."));
                }
            }
        }
        return (O) logger.exit(transformedData);
    }

    public enum DATA_TYPE {
        Boolean, Byte, ByteArray, Clob, Date, Double, Integer, Long, Short, String, UNKNOWN;

        public static DATA_TYPE valueOfString(String value) {
            DATA_TYPE valueObject = Enum.valueOf(DATA_TYPE.class, value);
            if (valueObject == null)
                return UNKNOWN;
            return valueObject;
        }
    }

}

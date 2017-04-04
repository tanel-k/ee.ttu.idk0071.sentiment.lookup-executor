package ee.ttu.idk0071.sentiment.utils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BeanUtils {
	public static class BeanAccessException extends Exception {
		private static final long serialVersionUID = 5397657962793303223L;
		
		public BeanAccessException(Throwable t) {
			super(t);
		}
	}

	/**
	 * Converts the specified POJO to a String map.<br/>
	 * The mapping process will only involve String properties.
	 * 
	 * @param bean the object to be converted into a map
	 * @param modifier the strategy for modifying property names
	 * @return the map representation of the object
	 * @throws BeanAccessException when the mapping fails
	 */
	public static Map<String, String> toMap(Object bean, PropertyNameModifier modifier) throws BeanAccessException
	{
		try {
			Map<String, String> result = new HashMap<String, String>();
			BeanInfo info = Introspector.getBeanInfo(bean.getClass());
			
			for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
				if (pd.getPropertyType().isAssignableFrom(String.class)) {
					Method reader = pd.getReadMethod();
					if (reader != null) {
						String key = modifier.apply(pd.getName());
						String value = String.valueOf(reader.invoke(bean));
						result.put(key, value);
					}
				}
			}
			
			return result;
		} catch (Throwable t) {
			throw new BeanAccessException(t);
		}
	}

	/**
	 * Converts the specified POJO to a String map.<br/>
	 * The mapping process will only involve String properties.<br/>
	 * Camel-case getters are mapped to hyphenized properties, i.e. getFirstName will result in a first-name property
	 * 
	 * @param bean the object to be converted into a map
	 * @return the map representation of the object
	 */
	public static Map<String, String> toMap(Object bean) throws BeanAccessException {
		return toMap(bean, new PropertyNameHyphenizer());
	}
}

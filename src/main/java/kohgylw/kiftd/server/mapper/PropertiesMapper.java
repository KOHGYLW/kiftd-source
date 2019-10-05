package kohgylw.kiftd.server.mapper;

import kohgylw.kiftd.server.model.Propertie;

public interface PropertiesMapper {
	
	int insert(final Propertie p);
	
	int deleteByKey(final String propertieKey);
	
	Propertie selectByKey(final String propertieKey);
	
	int update(final Propertie p);
	
}

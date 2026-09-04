package com.procurement.platform.observability;
import java.util.*;
public final class LogRedaction { private LogRedaction(){}
 private static final Set<String> SENSITIVE=Set.of("password","passwd","secret","token","accesstoken","refreshtoken","authorization","apikey","privatekey");
 public static Map<String,Object> redactSensitiveFields(Map<String,?> fields){Map<String,Object> out=new HashMap<>(); fields.forEach((k,v)->{String n=k.toLowerCase(Locale.ROOT).replace("_",""); out.put(k,SENSITIVE.contains(n)?"***REDACTED***":v);}); return out;}
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package examples.message;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author U
 */
public class CalcAgentPlus extends Agent 
{	
    static final Base64 base64 = new Base64();
    
    public String serializeObjectToString(Object object) throws IOException 
    {
        String s = null;
        
        try 
        {
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(arrayOutputStream);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);         
        
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            gzipOutputStream.close();
            
            objectOutputStream.flush();
            objectOutputStream.close();
            
            s = new String(base64.encode(arrayOutputStream.toByteArray()));
            arrayOutputStream.flush();
            arrayOutputStream.close();
        }
        catch(Exception ex){}
        
        return s;
    }
    
    public Object deserializeObjectFromString(String objectString) throws IOException, ClassNotFoundException 
    {
        Object obj = null;
        try
        {    
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(base64.decode(objectString));
            GZIPInputStream gzipInputStream = new GZIPInputStream(arrayInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
            obj =  objectInputStream.readObject();
            
            objectInputStream.close();
            gzipInputStream.close();
            arrayInputStream.close();
        }
        catch(Exception ex){}
        return obj;
    }
    
    protected void setup() 
    {     
        String serviceName = "calculator-agent";
        
  	try {
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName("cap");
            sd.setType("basic-calculator");
            sd.addProperties(new Property("operation1", "plus"));
            sd.addProperties(new Property("operation2", "minus"));
            sd.addProperties(new Property("operation3", "multiply"));
            sd.addProperties(new Property("operation4", "divide"));
            dfd.addServices(sd);
  		
            DFService.register(this, dfd);
  	}
  	catch (FIPAException fe) {
            fe.printStackTrace();
  	}
	//First set-up answering behaviour	
	addBehaviour(new CyclicBehaviour(this) 
	{
            public void action() {
                ACLMessage msg = receive();
                Calculation calc = new Calculation();
                
		if (msg != null) 
                {   
                    String msgContent = msg.getContent();
                    
                    System.out.println("\n[CalcAgentPlus] Message Received");
                    System.out.println("[CalcAgentPlus] Sender Agent   : " + msg.getSender());
                    System.out.println("[CalcAgentPlus] Message content [Base64 string]: " + msgContent);                    
                    
                    try
                    {
                        calc = (Calculation)deserializeObjectFromString(msgContent);
                    }
                    catch(Exception ex)
                    {
                        System.out.println("\n[CalcAgentPlus] StrToObj conversion error: " + ex.getMessage());
                    }
                    
                    if (calc.getOperation().equals("plus")) {
                        int result = calc.getOperand1() + calc.getOperand2();
                        System.out.println(calc.getOperand1());
                        System.out.println(calc.getOperand2());

                        calc.setResult(result);
                        calc.setSuccess(true);
                        calc.setInfo("Arithmetic operation: " + calc.getOperand1() + " + " + calc.getOperand2() + " = " + calc.getResult());
                        System.out.println("[CalcAgentPlus] Calculation result: " + 
                                            calc.getOperand1() + " + " + calc.getOperand2() + " = " + calc.getResult());

                        String strObj = ""; 
                        try
                        {
                            strObj = serializeObjectToString(calc);
                        }
                        catch (Exception ex)
                        {
                            System.out.println("\n[CalcAgentPlus] ObjToStr conversion error: " + ex.getMessage());
                        }

                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);

                        reply.addReceiver(msg.getSender()); //get from envelope                       

                        reply.setContent(strObj);                        
                        send(reply);

                        System.out.println("\n[CalcAgentPlus] Sending Message!");
                        System.out.println("[CalcAgentPlus] Receiver Agent                 : " + msg.getSender());
                        System.out.println("[CalcAgentPlus] Message content [Base64 string]: " + msg.getContent());
                    }
                    /*else if  (calc.getOperation().equals("minus")) {
                        int result = calc.getOperand1() - calc.getOperand2();
                        System.out.println(calc.getOperand1());
                        System.out.println(calc.getOperand2());

                        calc.setResult(result);
                        calc.setSuccess(true);
                        calc.setInfo("Arithmetic operation: " + calc.getOperand1() + " - " + calc.getOperand2() + " = " + calc.getResult());
                        
                        System.out.println("[CalcAgentPlus] Calculation result: " + 
                                            calc.getOperand1() + " - " + calc.getOperand2() + " = " + calc.getResult());

                        String strObj = ""; 
                        try
                        {
                            strObj = serializeObjectToString(calc);
                        }
                        catch (Exception ex)
                        {
                            System.out.println("\n[CalcAgentPlus] ObjToStr conversion error: " + ex.getMessage());
                        }

                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);

                        reply.addReceiver(msg.getSender()); //get from envelope                       

                        reply.setContent(strObj);                        
                        send(reply);

                        System.out.println("\n[CalcAgentPlus] Sending Message!");
                        System.out.println("[CalcAgentPlus] Receiver Agent                 : " + msg.getSender());
                        System.out.println("[CalcAgentPlus] Message content [Base64 string]: " + msg.getContent());                        
                    } 
                    else if  (calc.getOperation().equals("divide")) {
                        int result = calc.getOperand1() / calc.getOperand2();
                        System.out.println(calc.getOperand1());
                        System.out.println(calc.getOperand2());

                        calc.setResult(result);
                        calc.setSuccess(true);
                        calc.setInfo("Arithmetic operation: " + calc.getOperand1() + " / " + calc.getOperand2() + " = " + calc.getResult());
                        
                        System.out.println("[CalcAgentPlus] Calculation result: " + 
                                            calc.getOperand1() + " / " + calc.getOperand2() + " = " + calc.getResult());

                        String strObj = ""; 
                        try
                        {
                            strObj = serializeObjectToString(calc);
                        }
                        catch (Exception ex)
                        {
                            System.out.println("\n[CalcAgentPlus] ObjToStr conversion error: " + ex.getMessage());
                        }

                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);

                        reply.addReceiver(msg.getSender()); //get from envelope                       

                        reply.setContent(strObj);                        
                        send(reply);

                        System.out.println("\n[CalcAgentPlus] Sending Message!");
                        System.out.println("[CalcAgentPlus] Receiver Agent                 : " + msg.getSender());
                        System.out.println("[CalcAgentPlus] Message content [Base64 string]: " + msg.getContent());                        
                    }   
                    else if  (calc.getOperation().equals("multiply")) {
                        int result = calc.getOperand1() * calc.getOperand2();
                        System.out.println(calc.getOperand1());
                        System.out.println(calc.getOperand2());

                        calc.setResult(result);
                        calc.setSuccess(true);
                        calc.setInfo("Arithmetic operation: " + calc.getOperand1() + " x " + calc.getOperand2() + " = " + calc.getResult());
                        
                        System.out.println("[CalcAgentPlus] Calculation result: " + 
                                            calc.getOperand1() + " x " + calc.getOperand2() + " = " + calc.getResult());

                        String strObj = ""; 
                        try
                        {
                            strObj = serializeObjectToString(calc);
                        }
                        catch (Exception ex)
                        {
                            System.out.println("\n[CalcAgentPlus] ObjToStr conversion error: " + ex.getMessage());
                        }

                        ACLMessage reply = new ACLMessage(ACLMessage.INFORM);

                        reply.addReceiver(msg.getSender()); //get from envelope                       

                        reply.setContent(strObj);                        
                        send(reply);

                        System.out.println("\n[CalcAgentPlus] Sending Message!");
                        System.out.println("[CalcAgentPlus] Receiver Agent                 : " + msg.getSender());
                        System.out.println("[CalcAgentPlus] Message content [Base64 string]: " + msg.getContent());                        
                    }*/ else {
                        calc.setSuccess(false);
                        calc.setInfo("Operator not supported: " + calc.getOperation());
                        
                        System.out.println("Operator not supported: " + calc.getOperation());

                        String strObj = ""; 
                        try
                        {
                            strObj = serializeObjectToString(calc);
                        }
                        catch (Exception ex)
                        {
                            System.out.println("\n[CalcAgentPlus] ObjToStr conversion error: " + ex.getMessage());
                        }

                        ACLMessage reply = new ACLMessage(ACLMessage.NOT_UNDERSTOOD);

                        reply.addReceiver(msg.getSender()); //get from envelope                       

                        reply.setContent(strObj);                        
                        send(reply);

                        System.out.println("\n[CalcAgentPlus] Sending Message!");
                        System.out.println("[CalcAgentPlus] Receiver Agent                 : " + msg.getSender());
                        System.out.println("[CalcAgentPlus] Message content [Base64 string]: " + msg.getContent());                                  
                    }                    
		}
                
                System.out.println("[CalcAgentPlus] CyclicBehaviour Block");
                block();
            }
	});
    }
}


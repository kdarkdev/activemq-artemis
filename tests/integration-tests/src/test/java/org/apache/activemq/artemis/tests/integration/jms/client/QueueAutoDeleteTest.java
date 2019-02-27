/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.artemis.tests.integration.jms.client;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.postoffice.QueueBinding;
import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
import org.apache.activemq.artemis.tests.util.JMSTestBase;
import org.junit.Before;
import org.junit.Test;

/**
 * QueueAutoDeleteTest this tests that we can configure at the queue level auto-delete behaviour of auto created queues.
 */
public class QueueAutoDeleteTest extends JMSTestBase {


   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
   }

   @Override
   protected Configuration createDefaultConfig(boolean netty) throws Exception {
      //Set scan period over aggressively so tests do not have to wait too long.
      return super.createDefaultConfig(netty).setAddressQueueScanPeriod(10);
   }

   protected ConnectionFactory getCF() throws Exception {
      return cf;
   }

   @Test
   public void testAutoDelete() throws Exception {
      ConnectionFactory fact = getCF();
      Connection connection = fact.createConnection();
      connection.start();

      try {

         Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

         String testQueueName = getName();

         Queue queue = session.createQueue(testQueueName + "?auto-delete=true");
         ActiveMQDestination activeMQDestination = (ActiveMQDestination) queue;

         assertEquals(testQueueName, queue.getQueueName());
         assertEquals(true, activeMQDestination.getQueueAttributes().getAutoDelete());

         MessageProducer producer = session.createProducer(queue);
         producer.send(session.createTextMessage("hello1"));
         producer.send(session.createTextMessage("hello2"));

         QueueBinding queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertTrue(queueBinding.getQueue().isAutoDelete());
         assertEquals(2, queueBinding.getQueue().getMessageCount());

         MessageConsumer consumer = session.createConsumer(queue);
         Message message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertEquals(1, queueBinding.getQueue().getMessageCount());

         consumer = session.createConsumer(queue);
         message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         //Wait longer than scan period.
         Thread.sleep(20);

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertNull(queueBinding);




      } finally {
         connection.close();
      }
   }

   @Test
   public void testAutoDeleteOff() throws Exception {
      ConnectionFactory fact = getCF();
      Connection connection = fact.createConnection();
      connection.start();

      try {

         Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

         String testQueueName = getName();

         Queue queue = session.createQueue(testQueueName + "?auto-delete=false");
         ActiveMQDestination activeMQDestination = (ActiveMQDestination) queue;

         assertEquals(testQueueName, queue.getQueueName());
         assertEquals(false, activeMQDestination.getQueueAttributes().getAutoDelete());

         MessageProducer producer = session.createProducer(queue);
         producer.send(session.createTextMessage("hello1"));
         producer.send(session.createTextMessage("hello2"));

         QueueBinding queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertFalse(queueBinding.getQueue().isAutoDelete());
         assertEquals(2, queueBinding.getQueue().getMessageCount());

         MessageConsumer consumer = session.createConsumer(queue);
         Message message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertEquals(1, queueBinding.getQueue().getMessageCount());

         consumer = session.createConsumer(queue);
         message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         //Wait longer than scan period.
         Thread.sleep(20);

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertNotNull(queueBinding);
         assertEquals(0, queueBinding.getQueue().getMessageCount());

      } finally {
         connection.close();
      }
   }

   @Test
   public void testAutoDeleteDelay() throws Exception {
      ConnectionFactory fact = getCF();
      Connection connection = fact.createConnection();
      connection.start();

      try {

         Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

         String testQueueName = getName();

         Queue queue = session.createQueue(testQueueName + "?auto-delete=true&auto-delete-delay=500");
         ActiveMQDestination activeMQDestination = (ActiveMQDestination) queue;

         assertEquals(testQueueName, queue.getQueueName());
         assertEquals(Long.valueOf(500), activeMQDestination.getQueueAttributes().getAutoDeleteDelay());

         MessageProducer producer = session.createProducer(queue);
         producer.send(session.createTextMessage("hello1"));
         producer.send(session.createTextMessage("hello2"));

         QueueBinding queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertTrue(queueBinding.getQueue().isAutoDelete());
         assertEquals(500, queueBinding.getQueue().getAutoDeleteDelay());
         assertEquals(2, queueBinding.getQueue().getMessageCount());

         MessageConsumer consumer = session.createConsumer(queue);
         Message message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertEquals(1, queueBinding.getQueue().getMessageCount());

         consumer = session.createConsumer(queue);
         message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         //Wait longer than scan period, but less than delay
         Thread.sleep(50);

         //Check the queue has not been removed.
         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertNotNull(queueBinding);

         //Wait longer than auto delete delay
         Thread.sleep(550);

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertNull(queueBinding);


      } finally {
         connection.close();
      }
   }

   @Test
   public void testAutoDeleteMessageCount() throws Exception {
      ConnectionFactory fact = getCF();
      Connection connection = fact.createConnection();
      connection.start();

      try {

         Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

         String testQueueName = getName();

         Queue queue = session.createQueue(testQueueName + "?auto-delete=true&auto-delete-message-count=1");
         ActiveMQDestination activeMQDestination = (ActiveMQDestination) queue;

         assertEquals(testQueueName, queue.getQueueName());
         assertEquals(Long.valueOf(1), activeMQDestination.getQueueAttributes().getAutoDeleteMessageCount());

         MessageProducer producer = session.createProducer(queue);
         producer.send(session.createTextMessage("hello1"));
         producer.send(session.createTextMessage("hello2"));

         QueueBinding queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertTrue(queueBinding.getQueue().isAutoDelete());
         assertEquals(1, queueBinding.getQueue().getAutoDeleteMessageCount());
         assertEquals(2, queueBinding.getQueue().getMessageCount());

         MessageConsumer consumer = session.createConsumer(queue);
         Message message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         //Wait longer than scan period
         Thread.sleep(20);

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertNull(queueBinding);


      } finally {
         connection.close();
      }
   }

   @Test
   public void testAutoDeleteMessageCountDisabled() throws Exception {
      ConnectionFactory fact = getCF();
      Connection connection = fact.createConnection();
      connection.start();

      try {

         Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

         String testQueueName = getName();

         Queue queue = session.createQueue(testQueueName + "?auto-delete=true&auto-delete-message-count=-1");
         ActiveMQDestination activeMQDestination = (ActiveMQDestination) queue;

         assertEquals(testQueueName, queue.getQueueName());
         assertEquals(Long.valueOf(-1), activeMQDestination.getQueueAttributes().getAutoDeleteMessageCount());

         MessageProducer producer = session.createProducer(queue);
         for (int i = 0; i < 100; i++) {
            producer.send(session.createTextMessage("hello" + i));
         }

         QueueBinding queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertEquals(100, queueBinding.getQueue().getMessageCount());
         assertTrue(queueBinding.getQueue().isAutoDelete());
         assertEquals(-1, queueBinding.getQueue().getAutoDeleteMessageCount());

         MessageConsumer consumer = session.createConsumer(queue);
         Message message = consumer.receive(100);
         assertNotNull(message);
         message.acknowledge();

         consumer.close();

         //Wait longer than scan period
         Thread.sleep(20);

         queueBinding = (QueueBinding) server.getPostOffice().getBinding(SimpleString.toSimpleString(testQueueName));
         assertNull(queueBinding);


      } finally {
         connection.close();
      }
   }


}

/*
 *   Copyright 2014 Calytrix Technologies
 *
 *   This file is part of wantest.
 *
 *   NOTICE:  All information contained herein is, and remains
 *            the property of Calytrix Technologies Pty Ltd.
 *            The intellectual and technical concepts contained
 *            herein are proprietary to Calytrix Technologies Pty Ltd.
 *            Dissemination of this information or reproduction of
 *            this material is strictly forbidden unless prior written
 *            permission is obtained from Calytrix Technologies Pty Ltd.
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package wantest;

import java.util.ArrayList;
import java.util.List;

import wantest.events.Event;
import hla.rti1516e.ObjectInstanceHandle;

public class TestObject
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private ObjectInstanceHandle handle;
	private String objectName;
	private TestFederate creator;
	private long createTime;
	
	private List<Event> events;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public TestObject( ObjectInstanceHandle handle, String objectName )
	{
		this.handle = handle;
		this.objectName = objectName;
		this.creator = null;
		this.createTime = System.currentTimeMillis();
		this.events = new ArrayList<Event>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public ObjectInstanceHandle getHandle()
	{
		return this.handle;
	}

	public String getName()
	{
		return this.objectName;
	}
	
	
	public TestFederate getCreator()
	{
		return this.creator;
	}
	
	public void setCreator( TestFederate federate )
	{
		this.creator = federate;
		federate.addObject( this );
	}

	public long getCreateTime()
	{
		return this.createTime;
	}
	
	public void addEvent( Event event )
	{
		this.events.add( event );
	}
	
	public boolean isValid()
	{
		return this.creator != null;
	}
	
	public List<Event> getEvents()
	{
		return this.events;
	}
	
	public int getEventCount()
	{
		return this.events.size();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}

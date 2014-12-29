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

import hla.rti1516e.ObjectInstanceHandle;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TestFederate
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String federateName;
	private Map<String,TestObject> objects;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public TestFederate( String federateName )
	{
		this.federateName = federateName;
		this.objects = new HashMap<String,TestObject>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void addObject( TestObject object )
	{
		this.objects.put( object.getName(), object );
	}
	
	public boolean containsObject( ObjectInstanceHandle handle )
	{
		for( TestObject testObject : objects.values() )
		{
			if( testObject.getHandle().equals(handle) )
				return true;
		}
		
		return false;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public String getFederateName()
	{
		return this.federateName;
	}

	public Collection<TestObject> getObjects()
	{
		return this.objects.values();
	}
	
	public int getObjectCount()
	{
		return this.objects.size();
	}

	public String toString()
	{
		return this.federateName;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}

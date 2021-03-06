/* Copyright (c) 2008 AndroidNerds
 *
 * Written and Maintained by the random guys.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package com.androidnerds.tools.Messages;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewInflate;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class ConversationViewAdapter extends BaseAdapter
{
	private Context gCtx;
	private long[] messageIds;
	private int gCount;

	public ConversationViewAdapter( Context c, String sender )
	{
		gCtx = c;
		MessagesDbAdapter gDb = new MessagesDbAdapter( gCtx );
		gDb.open();
		parseCursor( gDb.fetchMessagesFromSender( sender ) );

		gDb.close();
	}

	public int getCount()
	{
		return gCount;
	}

	public Object getItem( int position )
	{
		return messageIds[ position ];
	}

	public long getItemId( int position )
	{
		return position;
	}

	public View getView( int position, View convertView, ViewGroup parent )
	{
		ViewInflate inflate = ViewInflate.from( gCtx );
		View view = inflate.inflate( R.layout.conversationlist, parent, false, null );

		MessagesDbAdapter gDb = new MessagesDbAdapter( gCtx );
		gDb.open();

		long id = messageIds[ position ];

		//query using the id.
		Cursor item = gDb.fetchMessage( id );
		item.moveTo( -1 );

		if( item.next() ) {
			String sender = item.getString( 1 );
			String body = item.getString( 2 );
			long timeMillis = item.getLong( 5 );
			int status = item.getInt( 3 );
			int direction = item.getInt( 4 );

			//See if the sender is one of the contacts.
			ContentResolver resolver = gCtx.getContentResolver();
			Cursor c = resolver.query( android.provider.Contacts.People.CONTENT_URI, null, android.provider.Contacts.PhonesColumns.NUMBER + "='" + sender + "'", null, Contacts.People.DEFAULT_SORT_ORDER );

			while( c.next() ) {
				//check to find the person in the cursor and set their phone number as such.
				sender = c.getString( c.getColumnIndex( android.provider.Contacts.PeopleColumns.NAME ) );
			}
			c.close();

			TextView gContent = ( TextView )view.findViewById( R.id.gContent );
			if( direction == 1 ) {
				sender = "Me";
			}

			//Format the time properly to show up in the conversation dialog.
			boolean useMinutes = false;
			java.util.Date gDate = new java.util.Date( timeMillis );
			java.util.Date now = new java.util.Date( System.currentTimeMillis() );
			SimpleDateFormat date = new SimpleDateFormat( "MM/dd/yy" );
			if( date.format( gDate ).equals( date.format( now )  ) ) useMinutes = true;
			SimpleDateFormat timeFormat = new SimpleDateFormat( "hh:mm a" );

			String theDate;
			if( useMinutes ) theDate = timeFormat.format( gDate );
			else theDate = date.format( gDate );

			//TODO: make the different pieces part of the same string so it wraps better, but also allow for the coloring.
			String contentString = new String( "(" +theDate + ") " + sender + " : " + body );
			SpannableString content = new SpannableString( contentString );
			if( direction == 1 ) content.setSpan( new ForegroundColorSpan( 0xff9999ff ), 0, contentString.indexOf( " :" ) + 2, 0 );
 			else content.setSpan( new ForegroundColorSpan( 0xffffff00 ), 0, contentString.indexOf( " :" ) + 2, 0 );

			gContent.setText( content );
		}

		item.close();
		gDb.close();
		
		return view;
	}

	public void parseCursor( Cursor result )
	{
		int i = 0;
		gCount = result.count();
		messageIds = new long[ gCount ];
		
		while( result.next() ) {
			messageIds[ i ] = result.getLong( 0 );
			i++;
		}
	}

}
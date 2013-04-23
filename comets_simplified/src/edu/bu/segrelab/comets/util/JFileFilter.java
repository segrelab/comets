package edu.bu.segrelab.comets.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.filechooser.FileFilter;

/**
 * A simple FileFilter class that works by filename extension,
 * like the one in the JDK demo called ExtensionFilter, which
 * has been announced to be supported in a future Swing release.
 * @author Sun Microsystems
 *
 */
public class JFileFilter extends FileFilter
{
	protected String description;
	protected List<String> exts = new ArrayList<String>();
	
	public void addType(String s)
	{
		exts.add(s);
	}
	
	/**
	 * Return true if the given file is accepted by this filter.
	 */
	public boolean accept(File f)
	{
		// Little trick: if you don't do this, only directory names
		// ending in one of the extensions appear in the window.
		if (f.isDirectory())
		{
			return true;
		}
		else if (f.isFile())
		{
			Iterator<String> it = exts.iterator();
			while (it.hasNext())
			{
				if (f.getName().endsWith(it.next()))
					return true;
			}
		}
		// A file that didn't match, or a weirdo (e.g. UNIX device file?).
		return false;
	}

	/**
	 * Set the printable description of this filter.
	 */
	public void setDescription(String s)
	{
		description = s;
	}
	
	/**
	 * Return the printable description of this filter.
	 */
	public String getDescription()
	{
		return description;
	}

}

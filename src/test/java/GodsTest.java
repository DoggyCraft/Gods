package test.java;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.enginehub.piston.CommandManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dogonfire.gods.Gods;
import com.dogonfire.gods.commands.GodsCommandExecuter;
import com.dogonfire.gods.config.GodsConfiguration;
import com.dogonfire.gods.managers.BelieverManager;
import com.dogonfire.gods.managers.GodManager;
import com.dogonfire.gods.managers.LanguageManager;
import com.dogonfire.gods.managers.PermissionsManager;
import com.dogonfire.gods.managers.QuestManager;
import com.dogonfire.gods.managers.WhitelistManager;

import static org.mockito.Mockito.*;


public class GodsTest
{
	@Test
	@DisplayName("Sanity check. Should always be true")
    public void testTrue() 
	{        
        //assertThat(os.toString(), is(equalTo(String.format("%s%s", Hello.HELLO, System.lineSeparator()))));
        assertTrue(true);
    }

	@Test
	@DisplayName("Static instance must always return a non-null value")
    public void testGodManagerInstance() 
	{        
        GodManager manager = GodManager.instance();

        assertNotNull(manager);
    }
	
	
//	@Test
//	@DisplayName("Enable() must load everything without error")
//    public void testEnable() 
//	{        
//		Gods plugin = mock(Gods.class);
//		
//		//plugin.getCommand("gods").setExecutor(GodsCommandExecuter.instance());
//	
//		GodsConfiguration.instance().loadSettings();
//		GodsConfiguration.instance().saveSettings();
//		PermissionsManager.instance().load();
//		LanguageManager.instance().load();
//		GodManager.instance().load();
//		QuestManager.instance().load();
//		BelieverManager.instance().load();
//		WhitelistManager.instance().load();
//	}
//	
//	@Test
//	@DisplayName("/gods info command")
//    public void testParseCommand()
//    {
//        System.out.println("* CommandManagerTest: testParseCommand()");
//
//        Gods plugin = mock(Gods.class);
//        PluginManager pManager = mock(PluginManager.class);
//
//        Server server = mock(Server.class);
//        when(server.getPluginManager()).thenReturn(pManager);
//
//        when(plugin.getServer()).thenReturn(server);
//
//        //GodManager manager = new GodManager();
//        //manager.registerCommands(this, false);
//
//        Player player = mock(Player.class);
//        when(player.getName()).thenReturn("DogOnFire");
//        
//        //manager.parseCommand(player, "test", new ArrayList<String>());
//
//        //plugin.getCommand("gods").setExecutor(GodsCommandExecuter.instance());
//        //plugin.onCommand(sender, command, label, args)
//        
//        //assertTrue(hasRun);
//    }

    //@Command(label = "test")
    //public void onTestCommand(Player player, List<String> args)
    //{
    //    hasRun = true;
    //}

	/*
	@Test
	@DisplayName("God manager must load config file handling any errors")
    public void testGodManagerLoad() 
	{        
        GodManager manager = GodManager.instance();

        
        try
        {
        	manager.load();
        }
        catch(Exception ex)
        {        	
        	fail("Error: " + ex.getMessage());
        }                     
    }
    */
}

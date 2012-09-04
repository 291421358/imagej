/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.command;

import imagej.event.EventService;
import imagej.module.Module;
import imagej.module.ModuleInfo;
import imagej.module.ModuleService;
import imagej.plugin.IPlugin;
import imagej.plugin.PluginService;
import imagej.plugin.PostprocessorPlugin;
import imagej.plugin.PreprocessorPlugin;
import imagej.service.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Interface for service that keeps track of available commands.
 * <p>
 * A <em>command</em> is a particular type of {@link IPlugin} that is also a
 * {@link Module}; i.e., it is {@link Runnable}, with typed inputs and outputs.
 * <p>
 * The command service keeps a master index of all commands known to the system.
 * It asks the {@link PluginService} for available commands, then takes care of
 * registering them with the {@link ModuleService}.
 * </p>
 * 
 * @author Curtis Rueden
 * @see IPlugin
 * @see ModuleService
 * @see PluginService
 */
public interface CommandService extends Service {

	EventService getEventService();

	PluginService getPluginService();

	ModuleService getModuleService();

	/** Gets the list of available {@link Command}s). */
	List<CommandInfo<Command>> getCommands();

	/** Gets the first available command of the given class, or null if none. */
	<C extends Command> CommandInfo<C> getCommand(
		Class<C> commandClass);

	/**
	 * Gets the first available command of the given class name, or null if none.
	 */
	CommandInfo<Command> getCommand(String className);

	/** Gets the list of commands of the given type. */
	<C extends Command> List<CommandInfo<C>>
		getCommandsOfType(Class<C> type);

	/**
	 * Gets the list of commands of the given class.
	 * <p>
	 * Most classes will have only a single match, but some special classes
	 * (such as imagej.legacy.LegacyPlugin) may match many entries.
	 * </p>
	 */
	<C extends Command> List<CommandInfo<C>>
		getCommandsOfClass(Class<C> commandClass);

	/**
	 * Gets the list of commands with the given class name.
	 * <p>
	 * Most classes will have only a single match, but some special classes
	 * (such as imagej.legacy.LegacyPlugin) may match many entries.
	 * </p>
	 */
	List<CommandInfo<Command>> getCommandsOfClass(
		String className);

	/**
	 * Populates any {@link Service} parameters for the given command instance,
	 * using services from this one's application context.
	 * 
	 * @return The {@link CommandInfo} associated with the given command.
	 */
	<C extends Command> CommandInfo<C> populateServices(final C command);

	/**
	 * Executes the first command of the given class name.
	 * 
	 * @param className Class name of the command to execute.
	 * @param inputs List of input parameter names and values. The expected order
	 *          is in pairs: an input name followed by its value, for each desired
	 *          input to populate. Leaving some inputs unpopulated is allowed.
	 *          Passing the name of an input that is not valid for the plugin, or
	 *          passing a value of a type incompatible with the associated input
	 *          parameter, will issue an error and ignore that name/value pair.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	Future<Module> run(String className, Object... inputs);

	/**
	 * Executes the first command of the given class name.
	 * 
	 * @param className Class name of the command to execute.
	 * @param inputMap Table of input parameter values, with keys matching the
	 *          plugin's input parameter names. Passing a value of a type
	 *          incompatible with the associated input parameter will issue an
	 *          error and ignore that value.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	Future<Module> run(String className, Map<String, Object> inputMap);

	/**
	 * Executes the first command of the given class.
	 * 
	 * @param <C> Class of the command to execute.
	 * @param commandClass Class object of the command to execute.
	 * @param inputs List of input parameter names and values. The expected order
	 *          is in pairs: an input name followed by its value, for each desired
	 *          input to populate. Leaving some inputs unpopulated is allowed.
	 *          Passing the name of an input that is not valid for the plugin, or
	 *          passing a value of a type incompatible with the associated input
	 *          parameter, will issue an error and ignore that name/value pair.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	<C extends Command> Future<CommandModule<C>> run(Class<C> commandClass,
		Object... inputs);

	/**
	 * Executes the first command of the given class.
	 * 
	 * @param <C> Class of the command to execute.
	 * @param commandClass Class object of the command to execute.
	 * @param inputMap Table of input parameter values, with keys matching the
	 *          plugin's input parameter names. Passing a value of a type
	 *          incompatible with the associated input parameter will issue an
	 *          error and ignore that value.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	<C extends Command> Future<CommandModule<C>> run(Class<C> commandClass,
		Map<String, Object> inputMap);

	/**
	 * Executes the given module, with pre- and postprocessing steps from all
	 * available {@link PreprocessorPlugin}s and {@link PostprocessorPlugin}s in
	 * the plugin index.
	 * 
	 * @param info The module to instantiate and run.
	 * @param inputs List of input parameter names and values. The expected order
	 *          is in pairs: an input name followed by its value, for each desired
	 *          input to populate. Leaving some inputs unpopulated is allowed.
	 *          Passing the name of an input that is not valid for the plugin, or
	 *          passing a value of a type incompatible with the associated input
	 *          parameter, will issue an error and ignore that name/value pair.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	Future<Module> run(ModuleInfo info, Object... inputs);

	/**
	 * Executes the given module, with pre- and postprocessing steps from all
	 * available {@link PreprocessorPlugin}s and {@link PostprocessorPlugin}s in
	 * the plugin index.
	 * 
	 * @param info The module to instantiate and run.
	 * @param inputMap Table of input parameter values, with keys matching the
	 *          {@link ModuleInfo}'s input parameter names. Passing a value of a
	 *          type incompatible with the associated input parameter will issue
	 *          an error and ignore that value.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	Future<Module> run(ModuleInfo info, Map<String, Object> inputMap);

	/**
	 * Executes the given module, with pre- and postprocessing steps from all
	 * available {@link PreprocessorPlugin}s and {@link PostprocessorPlugin}s in
	 * the plugin index.
	 * 
	 * @param module The module to run.
	 * @param inputs List of input parameter names and values. The expected order
	 *          is in pairs: an input name followed by its value, for each desired
	 *          input to populate. Leaving some inputs unpopulated is allowed.
	 *          Passing the name of an input that is not valid for the plugin, or
	 *          passing a value of a type incompatible with the associated input
	 *          parameter, will issue an error and ignore that name/value pair.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	<M extends Module> Future<M> run(M module, Object... inputs);

	/**
	 * Executes the given module, with pre- and postprocessing steps from all
	 * available {@link PreprocessorPlugin}s and {@link PostprocessorPlugin}s in
	 * the plugin index.
	 * 
	 * @param module The module to run.
	 * @param inputMap Table of input parameter values, with keys matching the
	 *          module's {@link ModuleInfo}'s input parameter names. Passing a
	 *          value of a type incompatible with the associated input parameter
	 *          will issue an error and ignore that value.
	 * @return {@link Future} of the module instance being executed. Calling
	 *         {@link Future#get()} will block until execution is complete.
	 */
	<M extends Module> Future<M> run(M module, Map<String, Object> inputMap);

}

using System;
using System.Collections.Generic;
using System.Linq;
using Xamarin.UITest;
using Xamarin.UITest.Configuration;

namespace Bit.UITests.Setup.SimulatorManager
{
    internal static class IosSimulatorsManager
    {

        public static iOSAppConfigurator SetDeviceByName(this iOSAppConfigurator configurator, string simulatorName)
        {
            var deviceId = GetDeviceId(simulatorName);
            return configurator.DeviceIdentifier(deviceId);
        }

        public static string GetDeviceId(string simulatorName)
        {
            if (!TestEnvironment.Platform.Equals(TestPlatform.Local))
            {
                return string.Empty;
            }

            // See below for the InstrumentsRunner class.
            IEnumerable<Simulator> simulators = new InstrumentsRunner().GetListOfSimulators();

            var simulator = simulators.FirstOrDefault(x => x.Name.Contains(simulatorName));

            if (simulator == null)
            {
                throw new ArgumentException("Could not find a device identifier for '" + simulatorName + "'.", "simulatorName");
            }

            return simulator.GUID;
        }
    }
}

import React from 'react';

import { AppIcon } from './AppIcon';
import AppleIcon from '@mui/icons-material/Apple';
import LockPersonIcon from '@mui/icons-material/LockPerson';
import PasswordIcon from '@mui/icons-material/Password';
import GitHubIcon from '@mui/icons-material/GitHub';
import { SvgIconOwnProps } from '@mui/material';
import TerminalIcon from '@mui/icons-material/Terminal';
import SmartphoneIcon from '@mui/icons-material/Smartphone';
import ImportantDevicesIcon from '@mui/icons-material/ImportantDevices';
import SecurityUpdateIcon from '@mui/icons-material/SecurityUpdate';

export const WebAppIcon = ImportantDevicesIcon;
export const NativeAppIcon = SmartphoneIcon;
export const MachineAppIcon = TerminalIcon;
export const SpaAppIcon = SecurityUpdateIcon;

export const types = {
    web: { icon: WebAppIcon },
    native: { icon: NativeAppIcon },
    machine: { icon: MachineAppIcon },
    spa: { icon: SpaAppIcon },
};

export const getAppIcon = (type?: string, props?: SvgIconOwnProps) => {
    if (!type) {
        return <AppIcon {...props} />;
    }

    if (type in types) {
        return React.createElement(types[type].icon, props);
    }

    return <AppIcon {...props} />;
};

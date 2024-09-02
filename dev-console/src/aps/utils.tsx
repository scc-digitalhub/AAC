import React from 'react';

import { ApIcon } from './ApIcon';
import LockPersonIcon from '@mui/icons-material/LockPerson';
import CloudSyncIcon from '@mui/icons-material/CloudSync';
import FolderSharedIcon from '@mui/icons-material/FolderShared';
import TerminalIcon from '@mui/icons-material/Terminal';
import RouteIcon from '@mui/icons-material/Route';

import { SvgIconOwnProps } from '@mui/material';

export const InternalApIcon = LockPersonIcon;

export const authorities = {
    internal: { icon: FolderSharedIcon },
    mapper: { icon: RouteIcon },
    script: { icon: TerminalIcon },
    webhook: { icon: CloudSyncIcon },
};

export const getApIcon = (authority?: string, props?: SvgIconOwnProps) => {
    if (!authority) {
        return <ApIcon {...props} />;
    }

    if (authority in authorities) {
        return React.createElement(authorities[authority].icon, props);
    }

    return <ApIcon {...props} />;
};

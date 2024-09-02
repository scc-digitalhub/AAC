import React from 'react';

import { IdpIcon } from './IdpIcon';
import AppleIcon from '@mui/icons-material/Apple';
import LockPersonIcon from '@mui/icons-material/LockPerson';
import PasswordIcon from '@mui/icons-material/Password';
import GitHubIcon from '@mui/icons-material/GitHub';
import GoogleIcon from '@mui/icons-material/Google';
import FacebookIcon from '@mui/icons-material/Facebook';
import VpnKeyIcon from '@mui/icons-material/VpnKey';
import AssuredWorkloadIcon from '@mui/icons-material/AssuredWorkload';
import BusinessIcon from '@mui/icons-material/Business';
import { SvgIconOwnProps } from '@mui/material';
import LocalPoliceIcon from '@mui/icons-material/LocalPolice';

export const AppleIdpIcon = AppleIcon;
export const InternalIdpIcon = LockPersonIcon;
export const PasswordIdpIcon = PasswordIcon;
export const GithubIdpIcon = GitHubIcon;
export const FacebookIdpIcon = FacebookIcon;
export const GoogleIdpIcon = GoogleIcon;
export const SamlIdpIcon = BusinessIcon;
export const OidcIdpIcon = AssuredWorkloadIcon;
export const WebAuthnIdpIcon = VpnKeyIcon;
export const OpenIdFedIcon = LocalPoliceIcon;
export const SpidIcon = LocalPoliceIcon;

export const authorities = {
    apple: { icon: AppleIdpIcon },
    internal: { icon: InternalIdpIcon },
    password: { icon: PasswordIdpIcon },
    github: { icon: GithubIdpIcon },
    facebook: { icon: FacebookIdpIcon },
    saml: { icon: SamlIdpIcon },
    webauthn: { icon: WebAuthnIdpIcon },
    google: { icon: GoogleIdpIcon },
    oidc: { icon: OidcIdpIcon },
    openidfed: { icon: OpenIdFedIcon },
    spid: { icon: SpidIcon },
};

export const getIdpIcon = (authority?: string, props?: SvgIconOwnProps) => {
    if (!authority) {
        return <IdpIcon {...props} />;
    }

    if (authority in authorities) {
        return React.createElement(authorities[authority].icon, props);
    }

    return <IdpIcon {...props} />;
};

import { defaultTheme } from 'react-admin';
import 'typeface-titillium-web';
// import 'typeface-roboto-mono';

const palette = {
    primary: {
        main: '#0066cc',
        dark: '#00478e',
        light: '#3384d6',
    },
    secondary: {
        main: '#b2b2b2',
        dark: '#7c7c7c',
        light: '#c1c1c1',
    },
};

export const theme = {
    ...defaultTheme,
    palette,
    typography: {
        fontFamily: ['"Titillium Web"', 'Geneva', 'Tahoma', 'sans-serif'].join(
            ','
        ),
        h1: {
            fontWeight: 700,
        },
        h2: {
            fontWeight: 700,
        },
        h3: {
            fontWeight: 700,
        },
        h4: {
            fontWeight: 700,
        },
        h5: {
            fontWeight: 700,
        },
        button: {
            fontFamily: [
                '"Titillium Web"',
                'Geneva',
                'Tahoma',
                'sans-serif',
            ].join(','),
            fontWeight: 600,
            textTransform: 'none' as const,
            fontSize: '0.90rem',
        },
        body1: {
            // fontFamily: ['Lora', 'serif'].join(','),
        },
        body2: {
            // fontFamily: ['Lora', 'serif'].join(','),
        },
        // Use the system font instead of the default Roboto font.
        // fontFamily: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Arial', 'sans-serif'].join(','),
    },
    components: {
        RaAppBar: {
            styleOverrides: {
                root: {
                    backgroundColor: palette.primary.main,
                    color: defaultTheme.palette?.secondary?.contrastText,
                },
            },
        },
        MuiFormHelperText: {
            styleOverrides: {
                root: {
                    marginBottom: '14px',
                },
            },
        },
    },
};

export default theme;

package mobileworkloads.jlagmarker.gui;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;

public class SuggestionSelection {
	private JFrame frmWorkloadMarkup;
	private JTextField textField;
	private JTextField textField_1;
	private JTextField textField_2;
	private JTextField textField_3;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SuggestionSelection window = new SuggestionSelection();
					window.frmWorkloadMarkup.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public SuggestionSelection() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmWorkloadMarkup = new JFrame();
		frmWorkloadMarkup.setTitle("Workload Markup");
		frmWorkloadMarkup.setBounds(100, 100, 986, 720);
		frmWorkloadMarkup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmWorkloadMarkup.getContentPane().setLayout(null);
		
		JSlider slider = new JSlider();
		slider.setValue(30);
		slider.setMaximum(50);
		slider.setBounds(266, 22, 142, 16);
		frmWorkloadMarkup.getContentPane().add(slider);
		
		JSlider slider_1 = new JSlider();
		slider_1.setValue(12);
		slider_1.setMaximum(50);
		slider_1.setBounds(266, 46, 142, 16);
		frmWorkloadMarkup.getContentPane().add(slider_1);
		
		JSlider slider_2 = new JSlider();
		slider_2.setMaximum(500);
		slider_2.setValue(100);
		slider_2.setBounds(266, 74, 142, 16);
		frmWorkloadMarkup.getContentPane().add(slider_2);
		
		textField = new JTextField();
		textField.setText("30");
		textField.setBounds(410, 22, 52, 19);
		frmWorkloadMarkup.getContentPane().add(textField);
		textField.setColumns(10);
		
		textField_1 = new JTextField();
		textField_1.setText("12");
		textField_1.setColumns(10);
		textField_1.setBounds(410, 46, 52, 19);
		frmWorkloadMarkup.getContentPane().add(textField_1);
		
		textField_2 = new JTextField();
		textField_2.setText("100");
		textField_2.setColumns(10);
		textField_2.setBounds(410, 74, 52, 19);
		frmWorkloadMarkup.getContentPane().add(textField_2);
		
		JLabel lblImageCompare = new JLabel("Comparison");
		lblImageCompare.setBounds(160, 22, 94, 15);
		frmWorkloadMarkup.getContentPane().add(lblImageCompare);
		
		JLabel lblStillPeriod = new JLabel("Still Frames");
		lblStillPeriod.setBounds(160, 47, 94, 15);
		frmWorkloadMarkup.getContentPane().add(lblStillPeriod);
		
		JLabel lblPixelIgnored = new JLabel("Pixel Ignored");
		lblPixelIgnored.setBounds(160, 74, 94, 15);
		frmWorkloadMarkup.getContentPane().add(lblPixelIgnored);
		
		JScrollBar scrollBar = new JScrollBar();
		scrollBar.setOrientation(JScrollBar.HORIZONTAL);
		scrollBar.setBounds(0, 675, 984, 16);
		frmWorkloadMarkup.getContentPane().add(scrollBar);
		
		JComboBox comboBox = new JComboBox();
		comboBox.setBackground(Color.WHITE);
		comboBox.setModel(new DefaultComboBoxModel(new String[] {"None", "System Clock"}));
		comboBox.setSelectedIndex(1);
		comboBox.setBounds(490, 36, 142, 20);
		frmWorkloadMarkup.getContentPane().add(comboBox);
		
		JLabel lblMask = new JLabel("Apply Image Mask");
		lblMask.setBounds(490, 14, 142, 15);
		frmWorkloadMarkup.getContentPane().add(lblMask);
		
		JButton btnNewMask = new JButton("New Mask");
		btnNewMask.setBounds(491, 65, 117, 25);
		frmWorkloadMarkup.getContentPane().add(btnNewMask);
		
		JButton btnNewButton = new JButton("<html>Refresh<br />Suggestions</html>");
		btnNewButton.setBounds(820, 16, 117, 77);
		frmWorkloadMarkup.getContentPane().add(btnNewButton);
		
		JLabel lblPlayVideoSection = new JLabel("Play Video Section");
		lblPlayVideoSection.setBounds(658, 14, 142, 15);
		frmWorkloadMarkup.getContentPane().add(lblPlayVideoSection);
		
		JButton btnPlay = new JButton("Play");
		btnPlay.setBounds(658, 37, 117, 25);
		frmWorkloadMarkup.getContentPane().add(btnPlay);
		
		JSeparator separator = new JSeparator();
		separator.setBounds(0, 118, 984, 9);
		frmWorkloadMarkup.getContentPane().add(separator);
		
		JLabel lblBrowseLags = new JLabel("Browse Lags");
		lblBrowseLags.setBounds(26, 12, 94, 15);
		frmWorkloadMarkup.getContentPane().add(lblBrowseLags);
		
		JButton btnPrevious = new JButton("Previous");
		btnPrevious.setBounds(12, 35, 117, 25);
		frmWorkloadMarkup.getContentPane().add(btnPrevious);
		
		JButton btnNext = new JButton("Next");
		btnNext.setBounds(12, 63, 117, 25);
		frmWorkloadMarkup.getContentPane().add(btnNext);
		
		textField_3 = new JTextField();
		textField_3.setText("24");
		textField_3.setColumns(10);
		textField_3.setBounds(12, 91, 117, 19);
		frmWorkloadMarkup.getContentPane().add(textField_3);
		
		JLabel lblLag = new JLabel("Lag 23");
		lblLag.setFont(new Font("Dialog", Font.BOLD, 16));
		lblLag.setBounds(12, 202, 71, 25);
		frmWorkloadMarkup.getContentPane().add(lblLag);
		
		JLabel lblLag_2 = new JLabel("Lag 24");
		lblLag_2.setFont(new Font("Dialog", Font.BOLD, 16));
		lblLag_2.setBounds(12, 362, 71, 25);
		frmWorkloadMarkup.getContentPane().add(lblLag_2);
		
		JLabel lblLag_1 = new JLabel("Lag 25");
		lblLag_1.setFont(new Font("Dialog", Font.BOLD, 16));
		lblLag_1.setBounds(12, 537, 71, 25);
		frmWorkloadMarkup.getContentPane().add(lblLag_1);

		JLabel lblImgLabel = new JLabel();
		lblImgLabel.setBounds(160, 172, 237, 180);
		frmWorkloadMarkup.getContentPane().add(lblImgLabel);

		
		BufferedImage myPicture = null;
		try {
			myPicture = ImageIO.read(new File("./res/bw_frame.jpg.png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Image dimg = myPicture.getScaledInstance(lblImgLabel.getWidth(), lblImgLabel.getHeight(), Image.SCALE_SMOOTH);
		lblImgLabel.setIcon(new ImageIcon(dimg));

	}
}
